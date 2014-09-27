var User = require('../models/device').model;

module.exports = {
    createNewDevice: function(req, res) {
        User.create({}, function(err, user) {
            if (err) {
                res.status(500).json(err);
            } else {
                res.status(200).json({'id': user._id});
            }
        });
    }
};
