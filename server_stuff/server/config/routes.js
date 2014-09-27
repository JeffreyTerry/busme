var _ = require('underscore'),
    deviceController = require('../app/controllers/device_controller'),
    busRouteController = require('../app/controllers/bus_route_controller');

// Stores a dictionary with route paths as keys and their corresponding static html files as values.
var URLToFileMap = {
  '/': 'home/home',
  '/fun': 'fun/fun'
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
  app.get('/api/routes/fromcurrent/:lat/:lng/:destination', function(req, res){
    busRouteController.fromCurrent(req.params.lat, req.params.lng, req.params.destination, res);
  });

  // get fastest routes from start to destination
  app.get('/api/routes/fromcustom/:start/:destination', function(req, res){
    busRouteController.fromCustom(req.params.start, req.params.destination, res);
  });

  // get user suggested routes
  app.get('/api/routes/default/:uid/:lat/:lng', function(req, res){
    busRouteController.fromCurrent(req.params.uid, req.params.lat, req.params.lng, res);
  });

  app.post('/api/newdevice', function(req, res){
    deviceController.createNewDevice(req, res);
  });

};
