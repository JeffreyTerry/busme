var _ = require('underscore'),
    deviceController = require('../app/controllers/device_controller'),
    busRouteController = require('../app/controllers/bus_route_controller'),
    routeDataController = require('../app/controllers/route_data_controller');

// Stores a dictionary with route paths as keys and their corresponding static html files as values.
var URLToFileMap = {
  '/': 'home/home'
};

// Renders the proper web page for all static pages by parsing the route from the req object.
var renderStaticPage = function(req, res){
  res.render(URLToFileMap[req.route.path], {
      title: 'Jeffrey Terry'
  });
};

module.exports = function(app, config){
/* Client Routes */
  // All static pages
  _.each(URLToFileMap, function(value, key){
    app.get(key, renderStaticPage);
  });

  // get a list of [lat, lng] pairs that trace out a given route on a map
  app.get('/api/data/routes/list/:route_num', function(req, res){
    routeDataController.getRouteLatLngs('route' + req.params.route_num, res);
  });

  // get a dictionary like {stop_name: [lat, lng]}
  app.get('/api/data/stops/dictionary/latlngs', function(req, res){
    routeDataController.getStopsDictionary(req, res);
  });

  // get a dictionary like {stop_name: stop_id}
  app.get('/api/data/stops/dictionary/ids', function(req, res){
    routeDataController.getStopToIdDictionary(req, res);
  });

  // checks that the client's device id is recognized by the server
  // returns {'valid': true/false}
  app.get('/api/checkdeviceid/:uid', function(req, res){
    deviceController.checkDeviceId(req.params.uid, res);
  });

  // gets a new device id and creates an object with that id in the database
  // returns {'id': 'someidstring'}
  app.get('/api/getdeviceid', function(req, res){
    deviceController.createNewDevice(res);
  });

  // checks that the client and server have the same data
  // returns {'valid': true/false}
  app.get('/api/checkdataversion/:version', function(req, res){
    routeDataController.checkDataVersion(req.params.version, res);
  });

  // gets the current data version
  // returns {'version': 'someversionstring'}
  app.get('/api/getdataversion', function(req, res){
    routeDataController.getDataVersion(req.params.version, res);
  });








  // DEPRECATED
  app.get('/api/data/route/:route_num', function(req, res){
    routeDataController.getRouteLatLngs('route' + req.params.route_num, res);
  });

  // DEPRECATED
  app.get('/api/data/stops', function(req, res){
    routeDataController.getStopsDictionary(req, res);
  });

  // DEPRECATED
  app.get('/api/newdevice', function(req, res){
    deviceController.createNewDevice(res);
  });

  // DEPRECATED SHOULD BE DONE CLIENT-SIDE
  app.get('/api/routes/fromcurrent/:uid/:lat/:lng/:destination', function(req, res){
    req.params.destination = req.params.destination.replace('_', ' ');
    busRouteController.fromCurrent(req.params.uid, req.params.lat, req.params.lng, req.params.destination, function(err, response){
      if(err) {
        res.json([err]);
      } else {
        res.json(response);
      }
    });
  });

  // DEPRECATED SHOULD BE DONE CLIENT-SIDE
  app.get('/api/routes/fromcustom/:uid/:start/:destination', function(req, res){
    req.params.start = req.params.start.replace('_', ' ');
    req.params.destination = req.params.destination.replace('_', ' ');
    busRouteController.fromCustom(req.params.uid, req.params.start, req.params.destination, function(err, response){
      if(err) {
        res.json([err]);
      } else {
        res.json(response);
      }
    });
  });

  // DEPRECATED SHOULD BE DONE CLIENT-SIDE
  app.get('/api/routes/default/:uid/:lat/:lng', function(req, res){
    busRouteController.fromDefault(req.params.uid, req.params.lat, req.params.lng, function(err, response){
      if(err) {
        res.json([err]);
      } else {
        res.json(response);
      }
    });
  });
};
