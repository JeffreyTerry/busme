var User = require('../models/device').model;


function insert_result_in_back(list, result) {
    list.push(result);
    var temp;
    for(var i = list.length - 2; i >= 0; i--) {
        if(result.timeDifference < list[i].timeDifference) {
            temp = list[i];
            list[i] = list[i + 1];
            list[i + 1] = temp;
        } else {
            break;
        }
    }
    return list;
}


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
    },
    getDefaultCards: function(uid, lat, lng, res) {
        var now = new Date();
        now = new Date(now.getTime() + ((now.getTimezoneOffset() * 60 * 1000) - (240 * 60 * 1000)));
        User.findById(uid, function(err, user) {
            if(err) {
                res.json({'err': err});
            } else {
                var searches = user.searches;
                var mostRelevantSearches = [];
                var maxNumOfSearchesToReturn = 10;
                for(var i = 0; i < searches.length; i++) {
                    var searchTime = new Date(searches[i].time);
                    var toAdd = searches[i];
                    toAdd.timeDifference = Math.abs((searchTime.getHours() - now.getHours()) * 60 + (searchTime.getMinutes() - now.getMinutes()));
                    if(mostRelevantSearches.length < maxNumOfSearchesToReturn) {
                        mostRelevantSearches = insert_result_in_back(mostRelevantSearches, toAdd);
                    } else if(toAdd.timeDifference < mostRelevantSearches[mostRelevantSearches.length - 1].timeDifference) {
                        mostRelevantSearches.pop();
                        mostRelevantSearches = insert_result_in_back(mostRelevantSearches, toAdd);
                    }
                }
                res.json(mostRelevantSearches);
            }
        });
    },
    saveSearch: function(uid, start_lat, start_lng, destination) {
        var now = new Date();
        now = new Date(now.getTime() + ((now.getTimezoneOffset() * 60 * 1000) - (240 * 60 * 1000)));
        User.findByIdAndUpdate(uid, {$push: {searches: {
            startlat: parseFloat(start_lat),
            startlng: parseFloat(start_lng),
            destquery: destination,
            time: now.getTime()
            }}}, function(err, res){
        });
    }
};
