var User = require('../models/device').model;


module.exports = {
    createNewDevice: function(res) {
        User.create({}, function(err, user) {
            if (err) {
                res.json({'err': err});
            } else {
                res.json({'id': user._id});
            }
        });
    },
    checkDeviceId: function(uid, res) {
        User.findById(uid, function(err, user) {
            if(err || user == null) {
                res.json({'valid': false});
            } else {
                res.json({'valid': true});
            }
        });
    }
};
