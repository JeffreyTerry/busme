var _ = require('underscore'),
    deviceController = require('../app/controllers/device_controller');

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

  app.get('/api/test', function(req, res){
    res.json({'msg': 'hey'});
  });

  // get fastest routes from current location to destination
  app.get('/api/routes/fromcurrent/:lat/:lng/:destination', function(req, res){
    res.json([{'next_bus': '23', 'route_number': '11', 'start': 'Gates Hall', 'destination': 'Seneca Commons'}]);
  });

  // get fastest routes from start to destination
  app.get('/api/routes/fromcustom/:start/:destination', function(req, res){
    res.json([{'next_bus': '23', 'route_number': '11', 'start': 'Gates Hall', 'destination': 'Seneca Commons'}]);
  });

  // get user suggested routes
  app.get('/api/routes/default/:uid/:lat/:lng', function(req, res){
    res.json([{'next_bus': '23', 'route_number': '11', 'start': 'Gates Hall', 'destination': 'Seneca Commons'}]);
  });

  app.post('/api/newdevice', function(req, res){
    deviceController.createNewDevice(req, res);
  });

};
