var User = require('../models/device').model;

module.exports = {
    createNewDevice: function(req, res) {
        User.create({}, function(err, user) {
            if (err) {
                res.json(err);
            } else {
                res.json({'id': user._id});
            }
        });
    }
};
