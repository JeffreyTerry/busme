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

  // get fastest routes from current location to destination
  app.get('/api/routes/fromcurrent/:uid/:lat/:lng/:destination', function(req, res){
    req.params.destination = req.params.destination.replace('_', ' ');
    // req.params.destination += ' Ithaca';
    busRouteController.fromCurrent(req.params.lat, req.params.lng, req.params.destination, res);
  });

  // get fastest routes from start to destination
  app.get('/api/routes/fromcustom/:uid/:start/:destination', function(req, res){
    req.params.start = req.params.start.replace('_', ' ');
    // req.params.start += ' Ithaca';
    req.params.destination = req.params.destination.replace('_', ' ');
    // req.params.destination += ' Ithaca';
    busRouteController.fromCustom(req.params.start, req.params.destination, res);
  });

  // get user suggested routes
  app.get('/api/routes/default/:uid/:lat/:lng', function(req, res){
    busRouteController.fromDefault(req.params.uid, req.params.lat, req.params.lng, res);
  });

  // get a list of [lat, lng] pairs that trace out a given route
  app.get('/api/data/route/:route_num', function(req, res){
    routeDataController.getRouteLatLngs('route' + req.params.route_num, res);
  });

  // get a list of [lat, lng] pairs that trace out a given route
  app.get('/api/data/allstops', function(req, res){
    routeDataController.getAllStops(req, res);
  });

  app.get('/api/newdevice', function(req, res){
    deviceController.createNewDevice(req, res);
  });

  app.post('/api/buslocation', function(req, res){
    deviceController.createNewDevice(req, res);
  });

};
