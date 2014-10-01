module.exports = {
    arraysAreEqual: function(object1, object2) {
        //For the first loop, we only check for types
        for (propName in object1) {
            //Check for inherited methods and properties - like .equals itself
            //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/hasOwnProperty
            //Return false if the return value is different
            if (object1.hasOwnProperty(propName) != object2.hasOwnProperty(propName)) {
                return false;
            }
            //Check instance type
            else if (typeof object1[propName] != typeof object2[propName]) {
                //Different types => not equal
                return false;
            }
        }
        //Now a deeper check using other objects property names
        for(propName in object2) {
            //We must check instances anyway, there may be a property that only exists in object2
                //I wonder, if remembering the checked values from the first loop would be faster or not 
            if (object1.hasOwnProperty(propName) != object2.hasOwnProperty(propName)) {
                return false;
            }
            else if (typeof object1[propName] != typeof object2[propName]) {
                return false;
            }
            //If the property is inherited, do not check any more (it must be equa if both objects inherit it)
            if(!object1.hasOwnProperty(propName))
              continue;

            //Now the detail check and recursion

            //object1 returns the script back to the array comparing
            /**REQUIRES Array.equals**/
            if (object1[propName] instanceof Array && object2[propName] instanceof Array) {
                       // recurse into the nested arrays
               if (!object1[propName].equals(object2[propName]))
                            return false;
            }
            else if (object1[propName] instanceof Object && object2[propName] instanceof Object) {
                       // recurse into another objects
                       //console.log("Recursing to compare ", object1[propName],"with",object2[propName], " both named \""+propName+"\"");
               if (!object1[propName].equals(object2[propName]))
                            return false;
            }
            //Normal value comparison for strings and numbers
            else if(object1[propName] != object2[propName]) {
               return false;
            }
        }
        //If everything passed, let's say YES
        return true;
    }
}
