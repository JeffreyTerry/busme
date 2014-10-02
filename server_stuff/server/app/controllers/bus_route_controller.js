var gm = require('googlemaps'),
    fs = require('fs'),
    http = require('http'),
    request = require('request'),
    _ = require('underscore'),
    arraysAreEqual = require('./lib').arraysAreEqual,
    User = require('../models/device').model;

// stopDictionary will look like {stop1name: [lat, lng], stop2name: [lat, lng], ...}
var stopDictionary = {};
// stopToTCATIdDictionary will look like {stop1name: stop1TcatId, stop2name: stop2TcatId, ...}
var stopToTcatIdDictionary = {};
// stopList will look like [[stop1name, lat, lng], [stop2name, lat, lng], ...]
var stopList = [];
function loadData() {
    fs.readFile(__dirname + '/../../public/data/stops_list.txt', function(err, data){
        if(!err) {
            stopList = JSON.parse(data);
        }
    });
    fs.readFile(__dirname + '/../../public/data/stops_dictionary.txt', function(err, data){
        if(!err) {
            stopDictionary = JSON.parse(data);
        }
    });
    fs.readFile(__dirname + '/../../public/data/stop_to_id_dictionary.txt', function(err, data){
        if(!err) {
            stopToTcatIdDictionary = JSON.parse(data);
        }
    });
}

function saveSearch(uid, start_lat, start_lng, destination) {
    var now = new Date();
    now = new Date(now.getTime() + ((now.getTimezoneOffset() * 60 * 1000) - (240 * 60 * 1000)));
    User.findByIdAndUpdate(uid, {$push: {searches: {
        startlat: parseFloat(start_lat),
        startlng: parseFloat(start_lng),
        destquery: destination,
        time: now.getTime()
        }}}, function(err, res){
    });
}

function distanceBetween(lat1,lon1,lat2,lon2) {
      var R = 6371; // Radius of the earth in km
      var dLat = deg2rad(lat2-lat1);  // deg2rad below
      var dLon = deg2rad(lon2-lon1);
      var a = 
            Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
            Math.sin(dLon/2) * Math.sin(dLon/2);
      var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
      var d = R * c; // Distance in km
      return d;
}

function deg2rad(deg) {
  return deg * (Math.PI/180)
}

function getLatLngForSearchTerms(searchTerms, cb){
    highest_score = ['', 0];
    searchTerms = searchTerms.split(' ');
    for(stop in stopDictionary) {
        var words_matched = 0;
        for(i in searchTerms){
            if(stop.toLowerCase().indexOf(searchTerms[i].toLowerCase()) != -1){
                words_matched++;
            }
        }
        if(words_matched > highest_score[1]){
            highest_score = [stop, words_matched];
        }
    }
    if (highest_score[1] != 0) {
        result = {};
        result.lat = stopDictionary[highest_score[0]][0];
        result.lng = stopDictionary[highest_score[0]][1];
        cb(undefined, result);
    } else {
        searchTerms += ' ithaca';
        gm.geocode(searchTerms, function(err, response){
            if(err) {
                cb(err);
            } else {
                if(response.results.length == 0) {
                    cb('address_not_found');
                    return;
                }
                result = {};
                result.lat = response.results[0].geometry.location.lat;
                result.lng = response.results[0].geometry.location.lng;
                cb(undefined, result);
            }
        });
    }
}

function addRoutesIfRelevant(existingRoutes, newRoutes) {
    var duplicate;
    for(var j = 0; j < newRoutes.length; j++) {
        duplicate = false;
        for(var i = 0; i < existingRoutes.length; i++) {
            if(existingRoutes[i].next_bus == newRoutes[j].next_bus && existingRoutes[i].route_numbers == newRoutes[j].route_numbers) {
                duplicate = true;
            }
        }
        if(!duplicate && newRoutes[j].travel_time < 90) {
            existingRoutes.push(newRoutes[j]);
        }
    }
    return existingRoutes;
}

function insert_result_obj_in_back(list, result) {
    list.push(result);
    var temp;
    for(var i = list.length - 2; i >= 0; i--) {
        if(result.timeDifference < list[i].timeDifference) {
            temp = list[i];
            list[i] = list[i + 1];
            list[i + 1] = temp;
        } else {
            break;
        }
    }
    return list;
}

function insert_stop_array_in_back(list, stop, distance) {
    list.push([stop, distance]);
    var temp;
    for(var i = list.length - 2; i >= 0; i--) {
        if(distance < list[i][1]) {
            temp = list[i];
            list[i] = list[i + 1];
            list[i + 1] = temp;
        } else {
            break;
        }
    }
    return list;
}


function findClosestStops(start_lat, start_lng, dest_lat, dest_lng, num_of_results) {
    var max_stop_count = num_of_results;
    var closest_stops_to_start = [];
    var closest_stops_to_dest = [];
    for(stop in stopDictionary) {
        lat_lng = stopDictionary[stop];
        var distance_start = distanceBetween(lat_lng[0], lat_lng[1], start_lat, start_lng);
        var distance_dest = distanceBetween(lat_lng[0], lat_lng[1], dest_lat, dest_lng);
        
        if(closest_stops_to_start.length < max_stop_count) {
            closest_stops_to_start = insert_stop_array_in_back(closest_stops_to_start, stop, distance_start);
        } else if(distance_start < closest_stops_to_start[closest_stops_to_start.length - 1][1]) {
            closest_stops_to_start.pop();
            closest_stops_to_start = insert_stop_array_in_back(closest_stops_to_start, stop, distance_start);
        }
        if(closest_stops_to_dest.length < max_stop_count) {
            closest_stops_to_dest = insert_stop_array_in_back(closest_stops_to_dest, stop, distance_dest);
        } else if(distance_dest < closest_stops_to_dest[closest_stops_to_dest.length - 1][1]) {
            closest_stops_to_dest.pop();
            closest_stops_to_dest = insert_stop_array_in_back(closest_stops_to_dest, stop, distance_dest);
        }
    }
    return [closest_stops_to_start, closest_stops_to_dest];
}

// time
function getNextBusForStops(start, dest, time, cb) {
    if(!(stopToTcatIdDictionary.hasOwnProperty(start) && stopToTcatIdDictionary.hasOwnProperty(dest))) {
        console.log(stopToTcatIdDictionary.hasOwnProperty(start), stopToTcatIdDictionary.hasOwnProperty(dest));
        console.log("ERROR", 'start:', start, ', stop:', stop);
        cb({'err': 'invalid stops'});
    } else {
        request.post({
            url: 'http://tcat.nextinsight.com/index.php',
            form: {
                'wml': '',
                'addrO': '',
                'latO': '',
                'lonO': '',
                'addrD': '',
                'latD': '',
                'lonD': '',
                'origin': '',
                'destination': '',
                'search': 'search',
                'fulltext': '',
                'radiusO': '',
                'radiusD': '',
                'addressid1': '',
                'addressid2': '',
                'start': stopToTcatIdDictionary[start],
                'end': stopToTcatIdDictionary[dest],
                'day': time.getDay(),
                'departure': 0,
                'starthours': time.getHours() % 12,
                'startminutes': time.getMinutes(),
                'startampm': (Math.floor(time.getHours() / 12) == 0? 0: 1),
                'customer': 1,
                'sort': 1,
                'transfers': 0,
                'addr': '',
                'city': 'Ithaca',
                'radius': .25
            }},
            function (error, response, body) {
                if (!error && response.statusCode == 200) {
                    body = body.replace(/<sup>(\w)*<\/sup>/g, '');
                    body = body.replace(':</strong>', '</strong>');
                    body = body.replace(':</b>', '</b>');
                    var nextBusStarts = body.match(/<[^<]*<[^<]*Board the[^<]*<[^<]*<[^<]*<a\shref="\/stops\/(\w)*">[^<]*<\/a>(<br>[\w\s,]*)?/g);
                    var nextBusDestinations = body.match(/<[^<]*<[^<]*Get off at[^<]*<a\shref="\/stops\/(\w)*">[^<]*<\/a>/g);
                    var nextBusTravelTimes = body.match(/[Ee]stimated\s*[Tt]rip\s*[Tt]ime:[\s\w]*/g);
                    if(nextBusStarts == null || nextBusDestinations == null || nextBusTravelTimes == null) {
                        // cb({'err': 'no routes found', 'nbs': nextBusStarts, 'nbds': nextBusDestinations, 'nbtts': nextBusTravelTimes, 'body': body, 'error': error});
                        cb({'err': 'no routes found'});
                    } else {
                        var nextBusStart = nextBusStarts[0];
                        var nextBusDestination = nextBusDestinations[0];
                        var nextBusRoutes = nextBusStart.match(/(Route)(\s)*(\d)*/g);
                        var nextBusRouteNumbers = [];
                        if(nextBusRoutes != null) {
                            var i = 0;
                            while(('' + nextBusRoutes[i]).match(/(Route)(\s)*(\d)*/) != null) {
                                var nextBusRouteNumber = nextBusRoutes[i].match(/(\d)*$/);
                                if(nextBusRouteNumber != null) {
                                    nextBusRouteNumbers.push(nextBusRouteNumber[0]);
                                }
                                i++;
                            }
                        }
                        if(nextBusRouteNumbers.length == 0) {
                            nextBusRouteNumbers = '0'
                        } else {
                            nextBusRouteNumbers = _.uniq(nextBusRouteNumbers);
                            var result = '';
                            for(var i = 0; i < nextBusRouteNumbers.length; i++) {
                                result += nextBusRouteNumbers[i] + ',';
                            }
                            nextBusRouteNumbers = result.substring(0, result.length - 1);
                        }

                        var nextBusStartTime = nextBusStart.match(/(\d)*:(\d)*((\s)*)+((\bAM\b)|(\bPM\b))/);
                        if(nextBusStartTime != null) {
                            nextBusStartTime = nextBusStartTime[0];
                        }
                        var nextBusDestTime = nextBusDestination.match(/(\d)*:(\d)*((\s)*)+((\bAM\b)|(\bPM\b))/);
                        if(nextBusDestTime != null) {
                            nextBusDestTime = nextBusDestTime[0];
                        }

                        var nextBusStartStopName = nextBusStart.match(/<a\shref="\/stops\/(\w)*">[^<]*<\/a>/);
                        if(nextBusStartStopName != null) {
                            nextBusStartStopName = nextBusStartStopName[0].match(/>[(\S)(\s)]*</);
                            if(nextBusStartStopName != null) {
                                if(nextBusStartStopName[0].length > 1){
                                    nextBusStartStopName = nextBusStartStopName[0].substring(1, nextBusStartStopName[0].length - 1);
                                }
                            }
                        }
                        var nextBusDestStopName = nextBusDestination.match(/<a\shref="\/stops\/(\w)*">[^<]*<\/a>/);
                        if(nextBusDestStopName != null) {
                            nextBusDestStopName = nextBusDestStopName[0].match(/>[(\S)(\s)]*</);
                            if(nextBusDestStopName != null) {
                                if(nextBusDestStopName[0].length > 1){
                                    nextBusDestStopName = nextBusDestStopName[0].substring(1, nextBusDestStopName[0].length - 1);
                                }
                            }
                        }

                        var nextBusStartLatLng = '0.0';
                        if(stopDictionary.hasOwnProperty(nextBusStartStopName)){
                            nextBusStartLatLng = stopDictionary[nextBusStartStopName];
                        }
                        var nextBusDestLatLng = '0.0';
                        if(stopDictionary.hasOwnProperty(nextBusDestStopName)){
                            nextBusDestLatLng = stopDictionary[nextBusDestStopName];
                        }

                        var nextBusTravelTime = nextBusTravelTimes[0];
                        var nextBusTravelHours = nextBusTravelTime.match(/\d*\s*hour/);
                        var nextBusTravelMinutes = nextBusTravelTime.match(/\d*\s*minutes/);
                        if(nextBusTravelHours != null) {
                            nextBusTravelHours = nextBusTravelHours[0].match(/\d*/);
                            if(nextBusTravelHours != null) {
                                nextBusTravelHours = nextBusTravelHours[0];
                            }
                        }
                        if(nextBusTravelMinutes != null) {
                            nextBusTravelMinutes = nextBusTravelMinutes[0].match(/\d*/);
                            if(nextBusTravelMinutes != null) {
                                nextBusTravelMinutes = nextBusTravelMinutes[0];
                            }
                        }
                        if(nextBusTravelMinutes != null) {
                            nextBusTravelMinutes = parseInt(nextBusTravelMinutes);
                        } else {
                            nextBusTravelMinutes = 0;
                        }
                        if(nextBusTravelHours != null) {
                            nextBusTravelMinutes += parseInt(nextBusTravelHours) * 60;
                        }
                        if(nextBusStartTime.indexOf(':') == 1) {
                            nextBusStartTime = '0' + nextBusStartTime;
                        }

                        var nextBus = {
                            'next_bus': nextBusStartTime,
                            'travel_time': '' + nextBusTravelMinutes,
                            'route_number': nextBusRouteNumbers, // deprecated
                            'route_numbers': nextBusRouteNumbers,
                            'start': nextBusStartStopName,
                            'destination': nextBusDestStopName,
                            'start_lat': nextBusStartLatLng[0],
                            'start_lng': nextBusStartLatLng[1],
                            'dest_lat': nextBusDestLatLng[0],
                            'dest_lng': nextBusDestLatLng[1]
                        };
                        cb(undefined, [nextBus]);
                    }
                } else {
                    cb({'err': 'tcat post error'});
                }
            }
        );
    }
}

loadData();
// setTimeout(function(){
//     getLatLngForSearchTerms('airport', function(err, response){
//         if(err){
//             console.log('error outer', err);
//         }
//         start_lat = response.lat;
//         start_lng = response.lng;
//         locations = getLatLngForSearchTerms('sage', function(err, response2){
//             if(err){
//                 console.log('error inner', err);
//             }
//             dest_lat = response2.lat;
//             dest_lng = response2.lng;
//             closest_stops = findClosestStops(start_lat, start_lng, dest_lat, dest_lng, 4);
//             console.log('closes stops:', closest_stops);
//             getNextBusForStops(closest_stops[0][0][0], closest_stops[1][0][0], function(err, response){
//                 console.log(err, response);
//             });
//         });
//     });
// }, 2000);


module.exports = {
    // the options include {numberOfNearbyStopsToLookAt, numberOfTimesToQuery, ignoreSearchInDatabase}
    fromCurrent: function(uid, start_lat, start_lng, destination, cb, options) {
        if(!options) {
            options = {};
        }
        locations = getLatLngForSearchTerms(destination, function(err, response2){
            if(err) {
                cb({'err': 'start address not found'});
            } else {
                dest_lat = response2.lat;
                dest_lng = response2.lng;
                var numOfResultsToReturn = 2;
                if(options.hasOwnProperty('numberOfNearbyStopsToLookAt')) {
                    numOfResultsToReturn = options.numberOfNearbyStopsToLookAt;
                }
                closest_stops = findClosestStops(start_lat, start_lng, dest_lat, dest_lng, numOfResultsToReturn);
                var results = [];
                var numResultsReturned = 0;


                // this looks at routes up to 30 minutes in advance
                var numberOfTimesToQuery = 4;
                if(options.hasOwnProperty('numberOfTimesToQuery')) {
                    numberOfTimesToQuery = options.numberOfTimesToQuery;
                }
                var timesToQuery = [];
                var now = new Date();
                var TEN_MINUTES = 10 * 60 * 1000;
                for(var i = 0; i < numberOfTimesToQuery; i++) {
                    timesToQuery.push(new Date(now.getTime() + ((now.getTimezoneOffset() * 60 * 1000) - (240 * 60 * 1000)) + i * TEN_MINUTES));
                }
                for(var i = 0; i < numOfResultsToReturn; i++) {
                    for(var j = 0; j < numOfResultsToReturn; j++) {
                        for(var k = 0; k < timesToQuery.length; k++) {
                            getNextBusForStops(closest_stops[0][i][0], closest_stops[1][j][0], timesToQuery[k], function(err, response){
                                if(!err) {
                                    addRoutesIfRelevant(results, response);
                                } else {
                                    console.log(err);
                                }
                                numResultsReturned++;
                                if(numResultsReturned == (numOfResultsToReturn * numOfResultsToReturn * timesToQuery.length)) {
                                    if(results.length == 0){
                                        cb({'err': err});
                                    } else {
                                        console.log(uid, start_lat, start_lng, destination);
                                        if(!options || !options.hasOwnProperty('ignoreSearchInDatabase') || !options.ignoreSearchInDatabase) {
                                            saveSearch(uid, start_lat, start_lng, destination);
                                        }
                                        cb(undefined, results);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        });
    }, fromCustom: function(uid, start, destination, cb, options) {
        getLatLngForSearchTerms(start, function(err, response){
            if(err) {
                cb({'err': 'destination address not found'});
            } else {
                start_lat = response.lat;
                start_lng = response.lng;
                module.exports.fromCurrent(uid, start_lat, start_lng, destination, cb, options);
            }
        });
    }, fromDefault: function(uid, lat, lng, cb, options) {
        if(!options) {
            options = {};
        }
        options.ignoreSearchInDatabase = true;
        options.numberOfTimesToQuery = 1;
        var now = new Date();
        now = new Date(now.getTime() + ((now.getTimezoneOffset() * 60 * 1000) - (240 * 60 * 1000)));
        User.findById(uid, function(err, user) {
            if(err) {
                cb({'err': err});
            } else if(user == null) {
                cb({'err': 'user not found'});
            } else {
                var searches = user.searches;
                var mostRelevantSearches = [];
                var maxNumOfSearchesToReturn = 10;

                // step 1: add only searches that happened on this day of the week
                for(var i = 0; i < searches.length; i++) {
                    var searchTime = new Date(searches[i].time);
                    if(searchTime.getDay() == now.getDay()) {
                        var toAdd = searches[i];
                        toAdd.timeDifference = Math.abs((searchTime.getHours() - now.getHours()) * 60 + (searchTime.getMinutes() - now.getMinutes()));
                        if(mostRelevantSearches.length < maxNumOfSearchesToReturn) {
                            mostRelevantSearches = insert_result_obj_in_back(mostRelevantSearches, toAdd);
                        } else if(toAdd.timeDifference < mostRelevantSearches[mostRelevantSearches.length - 1].timeDifference) {
                            mostRelevantSearches.pop();
                            mostRelevantSearches = insert_result_obj_in_back(mostRelevantSearches, toAdd);
                        }
                    }
                }

                // TODO step 2: if step 1 failed, let's search for places based on location

                // step 3: if steps 1 & 2 failed, let's just give them the most recent searches
                if(mostRelevantSearches.length == 0) {
                    for(var i = 0; i < searches.length; i++) {
                        var toAdd = searches[i];
                        toAdd.timeDifference = toAdd.time * (-1);
                        if(mostRelevantSearches.length < maxNumOfSearchesToReturn) {
                            mostRelevantSearches = insert_result_obj_in_back(mostRelevantSearches, toAdd);
                        } else if(toAdd.timeDifference < mostRelevantSearches[mostRelevantSearches.length - 1].timeDifference) {
                            mostRelevantSearches.pop();
                            mostRelevantSearches = insert_result_obj_in_back(mostRelevantSearches, toAdd);
                        }
                    }
                }

                var searchesFinished = 0;
                var results = [];
                for(var i = 0; i < mostRelevantSearches.length; i++) {
                    module.exports.fromCurrent(uid, mostRelevantSearches[i].startlat, mostRelevantSearches[i].startlng, mostRelevantSearches[i].destquery, function(err, response) {
                        searchesFinished++;
                        addRoutesIfRelevant(results, response);
                        if(searchesFinished >= mostRelevantSearches.length) {
                            if(results.length == 0){
                                cb({'err': err});
                            } else {
                                cb(undefined, results);
                            }
                        }
                    }, options);
                }
            }
        });
        // res.json([{'next_bus': '10:00 AM', 'travel_time': '420', 'route_number': '69', 'route_numbers': '69', 'start': 'Gates Hall', 'destination': 'Seneca Commons', 'start_lat': '42.4448765', 'start_lng': '-76.48081429999999', 'dest_lat': '42.4458765', 'dest_lng': '-76.48181429999999'}]);
    }, makeLocation: function(req, res) {
        console.log(req.body);
    }, test: function(varname, res) {
        switch(varname){
            case 'stopList':
            res.json(stopList);
            break;
            case 'stopToTcatIdDictionary':
            res.json(stopToTcatIdDictionary);
            break;
            case 'stopDictionary':
            res.json(stopDictionary);
            break;
            case 'http':
            // get the current date, adding three hours
            var now = new Date(new Date().valueOf() + 3600000 * 3);
            request.post({
                url: 'http://tcat.nextinsight.com/index.php',
                form: {
                    'wml': '',
                    'addrO': '',
                    'latO': '',
                    'lonO': '',
                    'addrD': '',
                    'latD': '',
                    'lonD': '',
                    'origin': '',
                    'destination': '',
                    'search': 'search',
                    'fulltext': '',
                    'radiusO': '',
                    'radiusD': '',
                    'addressid1': '',
                    'addressid2': '',
                    'start': stopToTcatIdDictionary['Baker Flagpole'],
                    'end': stopToTcatIdDictionary['Schwartz Performing Arts'],
                    'day': now.getDay(),
                    'departure': 0,
                    'starthours': now.getHours() % 12,
                    'startminutes': now.getMinutes(),
                    'startampm': (Math.floor(now.getHours() / 12) == 0? 0: 1),
                    'customer': 1,
                    'sort': 1,
                    'transfers': 0,
                    'addr': '',
                    'city': 'Ithaca',
                    'radius': .25
                }},
                function (error, response, body) {
                    body = body.replace(/<sup>(\w)*<\/sup>/g, '');
                    body = body.replace(':</strong>', '</strong>');
                    body = body.replace(':</b>', '</b>');
                    res.json({'content': body});
                });
            break;
            case 'time':
            now = new Date();
            now = new Date(now.getTime() + ((now.getTimezoneOffset() * 60 * 1000) - (240 * 60 * 1000)));
            res.json({'now': now, 'offset': now.getTimezoneOffset()});
            default:
            res.json({'err': 'idu'});
            break;
        }
    }
};
