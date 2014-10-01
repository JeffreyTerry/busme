var gm = require('googlemaps'),
    fs = require('fs'),
    http = require('http'),
    request = require('request');

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

function insert_stop_in_back(list, stop, distance) {
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
            closest_stops_to_start = insert_stop_in_back(closest_stops_to_start, stop, distance_start);
        } else if(distance_start < closest_stops_to_start[closest_stops_to_start.length - 1][1]) {
            closest_stops_to_start.pop();
            closest_stops_to_start = insert_stop_in_back(closest_stops_to_start, stop, distance_start);
        }
        if(closest_stops_to_dest.length < max_stop_count) {
            closest_stops_to_dest = insert_stop_in_back(closest_stops_to_dest, stop, distance_dest);
        } else if(distance_dest < closest_stops_to_dest[closest_stops_to_dest.length - 1][1]) {
            closest_stops_to_dest.pop();
            closest_stops_to_dest = insert_stop_in_back(closest_stops_to_dest, stop, distance_dest);
        }
    }
    return [closest_stops_to_start, closest_stops_to_dest];
}

function getNextBusForStops(start, dest, cb) {
    if(!(stopToTcatIdDictionary.hasOwnProperty(start) && stopToTcatIdDictionary.hasOwnProperty(dest))) {
        console.log(stopToTcatIdDictionary.hasOwnProperty(start), stopToTcatIdDictionary.hasOwnProperty(dest));
        console.log("ERROR", 'start:', start, ', stop:', stop);
        cb({'err': 'invalid stops'});
    } else {
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
                'start': stopToTcatIdDictionary[start],
                'end': stopToTcatIdDictionary[dest],
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
                if (!error && response.statusCode == 200) {
                    body = body.replace(/<sup>(\w)*<\/sup>/g, '');
                    body = body.replace(':</strong>', '</strong>');
                    body = body.replace(':</b>', '</b>');
                    var nextBusStarts = body.match(/<[^<]*<[^<]*Board the[^<]*<[^<]*<[^<]*<a\shref="\/stops\/(\w)*">[^<]*<\/a>/g);
                    var nextBusDestinations = body.match(/<[^<]*<[^<]*Get off at[^<]*<a\shref="\/stops\/(\w)*">[^<]*<\/a>/g);
                    var nextBusTravelTimes = body.match(/[Ee]stimated\s*[Tt]rip\s*[Tt]ime:[\s\w]*/g);
                    if(nextBusStarts == null || nextBusDestinations == null || nextBusTravelTimes == null) {
                        // cb({'err': 'no routes found', 'nbs': nextBusStarts, 'nbds': nextBusDestinations, 'nbtts': nextBusTravelTimes, 'body': body, 'error': error});
                        cb({'err': 'no routes found'});
                    } else {
                        var nextBusStart = nextBusStarts[0];
                        var nextBusDestination = nextBusDestinations[0];
                        var nextBusRouteNumber = nextBusStart.match(/(Route)(\s)*(\d)*/);
                        if(nextBusRouteNumber.length != null) {
                            nextBusRouteNumber = nextBusRouteNumber[0].match(/(\d)*$/);
                            if(nextBusRouteNumber.length != null) {
                                nextBusRouteNumber = nextBusRouteNumber[0];
                            }
                        }
                        var nextBusStartTime = nextBusStart.match(/(\d)*:(\d)*((\s)*)+((\bAM\b)|(\bPM\b))/);
                        if(nextBusStartTime.length != null) {
                            nextBusStartTime = nextBusStartTime[0];
                        }
                        var nextBusStartStopName = nextBusStart.match(/<a\shref="\/stops\/(\w)*">[^<]*<\/a>/);
                        if(nextBusStartStopName.length != null) {
                            nextBusStartStopName = nextBusStartStopName[0].match(/>[(\S)(\s)]*</);
                            if(nextBusStartStopName.length != null) {
                                if(nextBusStartStopName[0].length > 1){
                                    nextBusStartStopName = nextBusStartStopName[0].substring(1, nextBusStartStopName[0].length - 1);
                                }
                            }
                        }
                        var nextBusDestTime = nextBusDestination.match(/(\d)*:(\d)*((\s)*)+((\bAM\b)|(\bPM\b))/);
                        if(nextBusDestTime.length != null) {
                            nextBusDestTime = nextBusDestTime[0];
                        }
                        var nextBusDestStopName = nextBusDestination.match(/<a\shref="\/stops\/(\w)*">[^<]*<\/a>/);
                        if(nextBusDestStopName.length != null) {
                            nextBusDestStopName = nextBusDestStopName[0].match(/>[(\S)(\s)]*</);
                            if(nextBusDestStopName.length != null) {
                                if(nextBusDestStopName[0].length > 1){
                                    nextBusDestStopName = nextBusDestStopName[0].substring(1, nextBusDestStopName[0].length - 1);
                                }
                            }
                        }
                        var nextBusStartLatLng = '';
                        if(stopDictionary.hasOwnProperty(nextBusStartStopName)){
                            nextBusStartLatLng = stopDictionary[nextBusStartStopName];
                        }
                        var nextBusDestLatLng = '';
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
                            'route_number': nextBusRouteNumber,
                            'start': nextBusStartStopName,
                            'destination': nextBusDestStopName,
                            'start_lat': nextBusStartLatLng[0],
                            'start_lng': nextBusStartLatLng[1],
                            'dest_lat': nextBusDestLatLng[0],
                            'dest_lng': nextBusDestLatLng[1]
                        };
                        cb(undefined, [nextBus]);
                    }
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
    fromCurrent: function(uid, start_lat, start_lng, destination, res) {
        locations = getLatLngForSearchTerms(destination, function(err, response2){
            if(err) {
                res.json([{'err': 'start address not found'}]);
            } else {
                dest_lat = response2.lat;
                dest_lng = response2.lng;
                var numOfResultsToReturn = 2;
                closest_stops = findClosestStops(start_lat, start_lng, dest_lat, dest_lng, numOfResultsToReturn);
                var results = [];
                var numResultsReturned = 0;
                for(var i = 0; i < numOfResultsToReturn; i++) {
                    for(var j = 0; j < numOfResultsToReturn; j++) {
                        getNextBusForStops(closest_stops[0][i][0], closest_stops[1][j][0], function(err, response){
                            if(!err) {
                                results.push.apply(results, response);
                            } else {
                                console.log(err);
                            }
                            numResultsReturned++;
                            if(numResultsReturned == (numOfResultsToReturn * numOfResultsToReturn)) {
                                if(results.length == 0){
                                    res.json([{'err': err}]);
                                } else {
                                    res.json(results);
                                }
                            }
                        });
                    }
                }
            }
        });
    }, fromCustom: function(uid, start, destination, res) {
        getLatLngForSearchTerms(start, function(err, response){
            if(err) {
                res.json([{'err': 'destination address not found'}]);
            } else {
                start_lat = response.lat;
                start_lng = response.lng;
                module.exports.fromCurrent(uid, start_lat, start_lng, destination, res);
            }
        });
    }, fromDefault: function(uid, lat, lng, res) {
        res.json([{'next_bus': '10:00 AM', 'travel_time': '420', 'route_number': '69', 'start': 'Gates Hall', 'destination': 'Seneca Commons', 'start_lat': '42.4448765', 'start_lng': '-76.48081429999999', 'dest_lat': '42.4458765', 'dest_lng': '-76.48181429999999'}]);
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
            default:
            res.json({'err': 'idu'});
            break;
        }
    }
};
