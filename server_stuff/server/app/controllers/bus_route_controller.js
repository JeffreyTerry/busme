var gm = require('googlemaps'),
    fs = require('fs');

var routeData = {};
var stopData = {};
function loadData() {
    obj = {};
    fs.readFile(__dirname + '/../../public/data/route_data.txt', function(err, data){
        if(!err){
            routeData = JSON.parse(data);
        }
    });

    fs.readFile(__dirname + '/../../public/data/stops.txt', function(err, data){
        if(!err) {
            stopData = JSON.parse(data);
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

function getAddressLatLng(address, cb){
    highest_score = ['', 0];
    address = address.split(' ');
    for(stop in stopData) {
        var words_matched = 0;
        for(i in address){
            if(stop.toLowerCase().indexOf(address[i].toLowerCase()) != -1){
                words_matched++;
            }
        }
        if(words_matched > highest_score[1]){
            highest_score = [stop, words_matched];
        }
    }
    if (highest_score[1] != 0) {
        console.log(highest_score);
        result = {};
        result.lat = stopData[highest_score[0]][0];
        result.lng = stopData[highest_score[0]][1];
        cb(undefined, result);
    } else {
        console.log(address);
        address += ' ithaca';
        gm.geocode(address, function(err, response){
            console.log(response.results[0].geometry);
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
    for(stop in stopData) {
        lat_lng = stopData[stop];
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
    // console.log(closest_stops_to_start);
    // console.log(closest_stops_to_dest);
    return [closest_stops_to_start, closest_stops_to_dest];
}

var days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
function findBestPossibleBusRoutes(possible_starts_and_dests) {
    // step 1: find all routes that go from the possible starting and ending destinations
    var now = new Date();
    var day_of_the_week = days[now.getDay()];
    routeDataToday = routeData[day_of_the_week];
    possible_starts = possible_starts_and_dests[0];
    possible_dests = possible_starts_and_dests[1];
    routes_possible = [];
    for(i in possible_starts) {
        for(j in routeDataToday) {
            if(possible_starts[i][0] in routeDataToday[j]) {
                for(k in possible_dests) {
                    if(possible_dests[k][0] in routeDataToday[j]) {
                        var to_push = [j, possible_starts[i][0], possible_dests[k][0]];
                        routes_possible.push(to_push);
                        break;
                    }
                }
            }
        }
    }

    // step 2: get info for all possible routes
    var best_routes = [];
    for(i in routes_possible){
        var next_bus_time = 100000;
        var travel_time = 100000;
        var now_time = 10000;
        start_times = routeDataToday[routes_possible[i][0]][routes_possible[i][1]];
        for(j in start_times){
            // this puts the time into a single number
            var index_of_colon = start_times[j].indexOf(':');
            hour = parseInt(start_times[j].substring(0, index_of_colon));
            minute = parseInt(start_times[j].substring(index_of_colon + 1, index_of_colon + 3));
            half = start_times[j].substr(start_times.length - 2);
            if(hour == 0 && half == 'PM'){
                hour += 12;
            } else if(half == 'PM') {
                hour += 12;
            }
            start_time = hour * 100 + minute;
            now_time = now.getHours() * 100 + now.getMinutes();
            if(start_time > now_time) {
                next_bus_time = start_time;
                break;
            }
        }
        dest_times = routeDataToday[routes_possible[i][0]][routes_possible[i][2]];
        for(j in dest_times){
            // this puts the time into a single number
            var index_of_colon = dest_times[j].indexOf(':');
            hour = parseInt(dest_times[j].substring(0, index_of_colon));
            minute = parseInt(dest_times[j].substring(index_of_colon + 1, index_of_colon + 3));
            half = dest_times[j].substr(dest_times.length - 2);
            if(hour == 0 && half == 'PM'){
                hour += 12;
            } else if(half == 'PM') {
                hour += 12;
            }
            dest_time = hour * 100 + minute;
            now_time = now.getHours() * 100 + now.getMinutes();
            if(dest_time > next_bus_time) {
                travel_time = dest_time - next_bus_time;
                travel_time = travel_time % 100 + Math.floor(travel_time / 100) * 60;
                break;
            }
        }
        next_bus_hour = Math.floor(next_bus_time / 100);
        var next_bus_minutes = next_bus_time % 100 + next_bus_hour * 60;
        now_time_hour = Math.floor(now_time / 100);
        var now_time_minutes = now_time % 100 + now_time_hour * 60 - 240; // blaze 240 cuz we in ithaca and server in oregon
        next_bus_time = next_bus_minutes - now_time_minutes;
        if(routes_possible[i][1] != routes_possible[i][2] && next_bus_time != 59671) {
            best_routes.push({'next_bus': next_bus_time, 'travel_time': travel_time, 'route_number': routes_possible[i][0].substring(5, 7), 'start': routes_possible[i][1], 'destination': routes_possible[i][2], 'start_lat': stopData[routes_possible[i][1]][0], 'start_lng': stopData[routes_possible[i][1]][1], 'dest_lat': stopData[routes_possible[i][2]][0], 'dest_lng': stopData[routes_possible[i][2]][1]});
        }
    }
    return best_routes;
}


loadData();
// setTimeout(function(){
//     closest_stops = findClosestStops(42.431494, -76.49203199999999, 42.431494, -76.49203199999999, 40);
//     possible_buses = findBestPossibleBusRoutes(closest_stops);
// }, 2000);
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
    }
};
