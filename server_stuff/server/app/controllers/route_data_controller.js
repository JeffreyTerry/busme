var fs = require('fs');

// change this to make the android app download new data.
// always make later data versions bigger numbers by convention.
var CURRENT_DATA_VERSION = '1';

module.exports = {
    getRouteLatLngs: function(route_name, res) {
        try{
            console.log(route_name);
            fs.readFile(__dirname + '/../../public/data/routelatlngs/' + route_name + '.txt', function(err, data){
                if(!err){
                    routeData = JSON.parse(data);
                    res.json(routeData);
                } else {
                    res.json([{'error': 'data_parse_error'}])
                }
            });
        } catch (err) {
            res.json([{'error': 'route_not_found'}])
        }
    },
    getStopsDictionary: function(req, res) {
        try{
            fs.readFile(__dirname + '/../../public/data/stops_dictionary.txt', function(err, data){
                if(!err){
                    routeData = JSON.parse(data);
                    res.json(routeData);
                } else {
                    console.log('err', err);
                    res.json([['error', 'data_parse_error']])
                }
            });
        } catch (err) {
            res.json([{'error': 'file_error'}])
        }
    },
    getStopToIdDictionary: function(req, res) {
        try{
            fs.readFile(__dirname + '/../../public/data/stop_to_id_dictionary.txt', function(err, data){
                if(!err){
                    routeData = JSON.parse(data);
                    res.json(routeData);
                } else {
                    console.log('err', err);
                    res.json([['error', 'data_parse_error']])
                }
            });
        } catch (err) {
            res.json([{'error': 'file_error'}])
        }
    },
    checkDataVersion: function(version, res) {
        if(version == CURRENT_DATA_VERSION) {
            res.json({'valid': true});
        } else {
            res.json({'valid': false});
        }
    },
    getDataVersion: function(req, res) {
        res.json({'version': CURRENT_DATA_VERSION});
    }
};