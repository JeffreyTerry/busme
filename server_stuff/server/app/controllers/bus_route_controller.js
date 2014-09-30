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
        console.log(searchTerms);
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
        cb({'err': 'invalid stops'});
        return;
    }
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
            'day': 1,
            'departure': 0,
            'starthours': 10,
            'startminutes': 40,
            'startampm': 0,
            'customer': 1,
            'sort': 1,
            'transfers': 0,
            'addr': '',
            'city': 'Ithaca',
            'radius': .25
        }},
        function (error, response, body) {
            if (!error && response.statusCode == 200) {
                nextBusStarts = body.match(/<[^<]*<[^<]*Board the[^<]*<[^<]*<[^<]*<a\shref="\/stops\/(\w)*">[^<]*<\/a>/g);
                nextBusDestinations = body.match(/<[^<]*<[^<]*Get off at[^<]*<a\shref="\/stops\/(\w)*">[^<]*<\/a>/g);
                if(nextBusStarts == null || nextBusDestinations == null) {
                    
                }
                var nextBusStart = nextBusStarts[0];
                var nextBusDestination = nextBusDestinations[0];
                console.log(nextBusStart)
                console.log(nextBusDestination);

                // cb(undefined, nextBus);
            }
            // console.log(response);
        }
    );
}

loadData();
setTimeout(function(){
    locations = getLatLngForSearchTerms('airport', function(err, response){
        if(err){
            console.log('error outer', err);
        }
        start_lat = response.lat;
        start_lng = response.lng;
        locations = getLatLngForSearchTerms('sage', function(err, response2){
            if(err){
                console.log('error inner', err);
            }
            dest_lat = response2.lat;
            dest_lng = response2.lng;
            closest_stops = findClosestStops(start_lat, start_lng, dest_lat, dest_lng, 4);
            console.log('closes stops:', closest_stops);
            getNextBusForStops(closest_stops[0][0][0], closest_stops[1][0][0], function(err, response){
                console.log(err, response);
            });
        });
    });
}, 2000);
module.exports = {
    fromCurrent: function(start_lat, start_lng, destination, res) {
        getAddressLatLng(destination, function(err, response){
            if(err) {
                res.json([{'err': 'address_not_found'}]);
            } else {
                dest_lat = response.lat;
                dest_lng = response.lng;
                closest_stops = [];
                possible_buses = [];
                var max_num_of_stops = 5;
                while(possible_buses.length == 0 && max_num_of_stops <= 55) {
                    closest_stops = findClosestStops(start_lat, start_lng, dest_lat, dest_lng, max_num_of_stops);
                    possible_buses = findBestPossibleBusRoutes(closest_stops);
                    max_num_of_stops += 10;
                }
                if(possible_buses.length > 0){
                    res.json(possible_buses);
                } else {
                    res.json([{'err': 'no_routes_found'}]);
                }
            }
        });
    }, fromCustom: function(start, destination, res) {
        getAddressLatLng(start, function(err, response1){
            getAddressLatLng(destination, function(err, response){
                if(err) {
                    res.json([{'err': 'address_not_found'}]);
                } else {
                    start_lat = response1.lat;
                    start_lng = response1.lng;
                    dest_lat = response.lat;
                    dest_lng = response.lng;
                    closest_stops = [];
                    possible_buses = [];
                    var max_num_of_stops = 5;
                    while(possible_buses.length == 0 && max_num_of_stops <= 55) {
                        closest_stops = findClosestStops(start_lat, start_lng, dest_lat, dest_lng, max_num_of_stops);
                        possible_buses = findBestPossibleBusRoutes(closest_stops);
                        max_num_of_stops += 10;
                    }
                    if(possible_buses.length > 0){
                        res.json(possible_buses);
                    } else {
                        res.json([{'err': 'no_routes_found'}]);
                    }
                }
            });
        });
    }, fromDefault: function(uid, lat, lng, res) {
        res.json([{'next_bus': '3', 'travel_time': '25', 'route_number': '12', 'start': 'Gates Hall', 'destination': 'Seneca Commons', 'start_lat': '42.4448765', 'start_lng': '-76.48081429999999', 'dest_lat': '42.4458765', 'dest_lng': '-76.48181429999999'}]);
    }, makeLocation: function(req, res) {
        console.log(req.body);
    }
};
