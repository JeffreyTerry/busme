var mongoose = require('mongoose');
exports.mongoose = mongoose;

var Schema = mongoose.Schema,
  ObjectId = Schema.ObjectId;

// user
var userSchema = mongoose.Schema({});

// Export blog post model
var userModel = mongoose.model('user', userSchema);
exports.model = userModel;

