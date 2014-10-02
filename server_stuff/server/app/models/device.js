var mongoose = require('mongoose');
exports.mongoose = mongoose;

var Schema = mongoose.Schema;

// startlat and startlng represent the starting location for a search
// destquery is what the user typed into the app for a search
// time is the time (in milliseconds since Jan 1, 1970) for which the request was made
var userSchema = mongoose.Schema({
    searches: [{
        startlat: Number,
        startlng: Number,
        destquery: String,
        time: Number
    }]
});

// Export blog post model
var userModel = mongoose.model('user', userSchema);
exports.model = userModel;

