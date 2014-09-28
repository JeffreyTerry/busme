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

function findBestPossibleBusRoutes(possible_starts_and_dests) {
    // possible_starts = possible_starts_and_dests[0];
    // possible_dests = possible_starts_and_dests[1];
    // routes_possible = [];
    // for(possible_start in possible_starts) {
    //     for(route in routeData) {
    //         if(possible_start in route) {
    //             for(possible_dest in possible_dests) {
    //                 if(possible_dest in route) {
    //                     routes_possible.append([route, possible_start, possible_dest])
    //                 }
    //             }
    //         }
    //     }
    // }
}

function getAddressLatLng(address, cb){
    gm.geocode(address, function(err, response){
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

var days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
// var today = new Date();
// var day_of_the_week = days[today.getDay()];
// var closest_stops = [];

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
            closest_stops_to_start = insert_stop_in_back(closest_stops_to_dest, stop, distance_dest);
        } else if(distance_dest < closest_stops_to_dest[closest_stops_to_dest.length - 1][1]) {
            closest_stops_to_dest.pop();
            closest_stops_to_start = insert_stop_in_back(closest_stops_to_dest, stop, distance_dest);
        }
    }
    // console.log(closest_stops_to_start);
    // console.log(closest_stops_to_dest);
    return [closest_stops_to_start, closest_stops_to_dest];
}

function deg2rad(deg) {
  return deg * (Math.PI/180)
}


loadData();
setTimeout(function(){
    closest_stops = findClosestStops(42.46816161469703, -76.54121641935363, 42.46816161469703, -76.54121641935363, 40);
    possible_buses = findBestPossibleBusRoutes(closest_stops);
}, 2000);
module.exports = {
    fromCurrent: function(start_lat, start_lng, destination, res) {
        getAddressLatLng(destination, function(err, response){
            if(err) {
                res.json({'err': 'destination_address_not_found_error'});
            } else {
                dest_lat = response.lat;
                dest_lng = response.lng;
                res.json([{'next_bus': '23', 'travel_time': '30', 'route_number': '10', 'start': 'Gates Hall', 'destination': 'Seneca Commons', 'start_lat': '42.4448765', 'start_lng': '-76.48081429999999', 'dest_lat': '42.4458765', 'dest_lng': '-76.48181429999999'}]);
            }
        });
    }, fromCustom: function(start, destination, res) {
        getAddressLatLng(destination, function(err, response){
            if(err) {
                res.json({'err': 'destination_address_not_found_error'});
            } else {
                dest_lat = response.lat;
                dest_lng = response.lng;
                res.json([{'next_bus': '17', 'travel_time': '15', 'route_number': '11', 'start': 'Gates Hall', 'destination': 'Seneca Commons', 'start_lat': '42.4448765', 'start_lng': '-76.48081429999999', 'dest_lat': '42.4458765', 'dest_lng': '-76.48181429999999'}]);
            }
        });
    }, fromDefault: function(uid, lat, lng, res) {
        res.json([{'next_bus': '3', 'travel_time': '25', 'route_number': '12', 'start': 'Gates Hall', 'destination': 'Seneca Commons', 'start_lat': '42.4448765', 'start_lng': '-76.48081429999999', 'dest_lat': '42.4458765', 'dest_lng': '-76.48181429999999'}]);
    }
};
