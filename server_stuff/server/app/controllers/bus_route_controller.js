var gm = require('googlemaps');

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
      return d * 1000;
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
            console.log(response);
            result = {};
            result.lat = response.results[0].geometry.location.lat;
            result.lng = response.results[0].geometry.location.lng;
            cb(undefined, result);
        }
    });
};

function deg2rad(deg) {
  return deg * (Math.PI/180)
}

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
    }, default: function(uid, lat, lng, res) {
        res.json([{'next_bus': '3', 'travel_time': '25', 'route_number': '12', 'start': 'Gates Hall', 'destination': 'Seneca Commons', 'start_lat': '42.4448765', 'start_lng': '-76.48081429999999', 'dest_lat': '42.4458765', 'dest_lng': '-76.48181429999999'}]);
    }
};
