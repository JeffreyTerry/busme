var fs = require('fs');

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
    getAllStops: function(req, res) {
        try{
            fs.readFile(__dirname + '/../../public/data/stop_names.txt', function(err, data){
                if(!err){
                    routeData = JSON.parse(data);
                    res.json(routeData);
                } else {
                    res.json([{'error': 'data_parse_error'}])
                }
            });
        } catch (err) {
            res.json([{'error': 'file_error'}])
        }
    }
};