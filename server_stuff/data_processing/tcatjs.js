

    var displayedRoutes = new Array('10'); //an array of the route numbers to display at pageload
    
    var routesDowntown = new Array('10','11','15','17');
    var routesCornell = new Array('81','82','83','92');
    var routesSuburbs = new Array('13','14','30','31','32','51');
    var routesRural = new Array('20','21','36','37','40','41','43','52','53','65','67');
    var routesWeekend = new Array('70','72','74','75','77');
    var routesEvening = new Array('90','93');
    var routesSummer = new Array('22');

    
//-------------------- You do not need to edit anything below this line ------------------------------------------//
    
    var kmlDataURL = 'http://www.tcatbus.com/kml/';  //make sure it has the trailing slash
    var numberOfRoutes = 91; //upper limit of highest bus route
    var routeKmlData = new Array(numberOfRoutes);
    var toggleState = new Array(numberOfRoutes);
    var map;
    //var allRoutes = new Array('routesDowntown','routesCornell','routesSuburbs','routesRural','routesWeekend','routesEvening''routesSummer');

function buildMenu(routesName)
{
    var routesArray = eval(routesName);
    var divheadername = routesName+"Header";
    var displaySet = false;
    var div = document.getElementById(routesName);
    var html;
    
    html = "<ul>";
    if (routesArray.length>=1) {
        
        for (i=0;i<routesArray.length;i++) 
        {
            
            
            //looks for initially displayed routes
            for (g=0;g<displayedRoutes.length;g++) {
                if ((displayedRoutes[g] == routesArray[i]) && displaySet == false) {
                    displaySet = true;
                }
            }
            
            
            html = html + '<li><input type="checkbox" value="1" onclick="toggleKml(\''+routesArray[i]+'\');" id="c'+routesArray[i]+'" /> <span class="route r'+routesArray[i]+'">Route '+routesArray[i]+'<\/span><\/li>';
        
        }
        html = html + "<\/ul>";
        div.innerHTML = html;
    } else {
        div.innerHTML = "<p>No Routes<\/p>";
    }
    
    //sets hidden if none of the route numbers are displayed initially
    if (displaySet==true) {
        expand(routesName);
    }
    

}


function initializeKmlData()
{
    var checkbox = null;
    
    //load default data into the array and display it
    for (i=1;i<=routeKmlData.length;i++) {
        for (g=0;g<displayedRoutes.length;g++) {
            if (i==displayedRoutes[g]) { //if match then load the KML data

                // routeKmlData[i] = new GGeoXml(kmlDataURL+"route"+i+".kml");
                routeKmlData[i] = new google.maps.KmlLayer(kmlDataURL+"route"+i+".kml", { suppressInfoWindows: true });
                routeKmlData[i].setMap(map);
                
                toggleState[i] = 1;
                // map.addOverlay(routeKmlData[i]);
                
                checkbox = document.getElementById("c"+i);
                if (checkbox) {
                    checkbox.checked = true;
                }
            }
        }
    }

}


function toggleKml(routeNumber) {
    if (!routeKmlData[routeNumber]) {
        // routeKmlData[routeNumber] = new GGeoXml(kmlDataURL+"route"+routeNumber+".kml");
        routeKmlData[routeNumber] = new google.maps.KmlLayer(kmlDataURL+"route"+routeNumber+".kml", { suppressInfoWindows: true });
    }
  if (toggleState[routeNumber] == 1) {
    routeKmlData[routeNumber].setMap();
    // map.removeOverlay(routeKmlData[routeNumber]);
    toggleState[routeNumber] = 0;
  } else {
    routeKmlData[routeNumber].setMap(map);
    // map.addOverlay(routeKmlData[routeNumber]);
    toggleState[routeNumber] = 1;
  }
 }
 
 function expand(thisDiv)
 {
    var header = null;
    changeMarker(thisDiv,'[+]','[-]');
    new Effect.toggle(thisDiv,'slide',{duration:0.25});
 }