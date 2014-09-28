import urllib2
import re
import sys
from time import sleep
from pykml import parser
from pyquery import PyQuery as pq

def transform_coordinate_triplets(coordinate_list):
    return list((coordinate_list[i:i + 2][::-1] for i in xrange(0, len(coordinate_list), 3)))

def transform_coordinate_quartets(coordinate_list):
    return list((coordinate_list[i:i + 3][::-1] for i in xrange(0, len(coordinate_list), 4)))

# these are the routes we will collect data for
routes = [10, 11, 15, 17, 81, 82, 83, 92]
# routes = []

for route in routes:
    data = urllib2.urlopen('http://www.tcatbus.com/kml/route' + str(route) + '.kml').read();
    # data = open('data.txt').read()
    root = parser.fromstring(data)
    coordinate_data = str(root.Document.Placemark.LineString.coordinates).strip()
    coordinate_data = re.compile('[,\s]').split(coordinate_data)
    coordinate_list = transform_coordinate_triplets(coordinate_data)
    coordinate_list = str(coordinate_list)
    coordinate_list = coordinate_list.replace("'", '"')
    open('data/route' + str(route) + '.txt', 'w').write(coordinate_list)
    # print coordinate_list
    # sleep so the server doesn't get suspicious and kick us out
    sleep(0.5)

# these are the routes we will collect data for
# stops = ["Cornell", "Downtown", "Country"]
stops = []
full_coordinate_list = []
for stop in stops:
    data = urllib2.urlopen('http://www.tcatbus.com/kml/route' + stop + '.kml').read();
    # data = open('data.txt').read()
    root = parser.fromstring(data)
    placemarks = root.Document.Folder.Placemark
    coordinate_data = ''
    for placemark in placemarks:
        coordinate_data += unicode(placemark.name).encode('ascii', 'ignore') + ',' + str(placemark.Point.coordinates) + ','
    coordinate_data = coordinate_data[:-1].strip()
    coordinate_data = coordinate_data.replace(', ', ' ')
    coordinate_data = re.compile('[,]').split(coordinate_data)
    coordinate_list = transform_coordinate_quartets(coordinate_data)
    full_coordinate_list.extend(coordinate_list)
    # sleep so the server doesn't get suspicious and kick us out
    sleep(2)
print full_coordinate_list
coordinate_dictionary = {}
for coordinates in full_coordinate_list:
    coordinate_dictionary[coordinates[2]] = coordinates[:2]
if len(full_coordinate_list) > 0:
    open('data/stops.txt', 'w').write(str(coordinate_dictionary)) 

# a dictionary where each entry is (e.g.) {"route11MON" + n where n is some integer: value}
# the values are dictionaries where each entry is (e.g.) {"stop name": [times]}
routes = {}
# these are the numbers for each route to query on tcat's website
# route_schedules = [428]
route_schedules = []
for route_schedule in route_schedules:
    # data = urllib2.urlopen('http://tcat.nextinsight.com/routes/' + route_schedule).read();
    # open('schedule_test_data.txt', 'w').write(data)
    data = open('schedule_test_data.txt').read()
    data = data.replace('---', '--')

    # parse the data and put it into a pyquery object
    d = pq(data)
    tables = d('table').filter(lambda i: i > 1)
    # for each table of schedules (different outbound/inbound & day of week of routes for each table)
    for index, table in enumerate(tables.items()):
        # an array containing each stop td element in the table
        stops = list(table.children('tr').filter(lambda i: i == 1).children('td'))

        # a matrix representing the time td elements in the table
        times_matrix = []
        for j in range(4, len(table.children('tr'))):
            times_matrix.append(list(table.children('tr').filter(lambda i: i == j).children('td')))

        table_stops = []
        table_times = []
        for time_index, times in enumerate(times_matrix):
            potential_stops_to_add = []
            potential_times_to_add = []
            for index, time_td in enumerate(times):
                if str(time_td.text_content()) != '--' and index < len(stops):
                    potential_stops_to_add.append(stops[index].text_content())
                    potential_times_to_add.append(time_td.text_content())
            if not (potential_stops_to_add in table_stops):
                table_stops.append([])
                table_times.append([])
                table_stops[len(table_stops) - 1].extend(potential_stops_to_add)
                table_times[len(table_times) - 1].extend(potential_times_to_add)
            else:
                table_times[table_stops.index(potential_stops_to_add)].extend(potential_times_to_add)

        # this will be something like 'Outbound/Inbound from the center of Ithaca' or 'Loop'
        table_titles = list(table.children('tr').children('td').children('h6'))[0].text_content()

        # this will be something like 'Route 11 Monday - Wednesday'
        table_day_range = list(table.children('h5'))[0].text_content()
        
        # parse the days from the date range
        route_days = []
        if 'Weekday' in table_day_range or ('Monday' in table_day_range and 'Friday' in table_day_range):
            route_days = ['MON', 'TUE', 'WED', 'THU', 'FRI']
        elif 'Weekend' in table_day_range or ('Saturday' in table_day_range and 'Sunday' in table_day_range):
            route_days = ['SUN', 'SAT']
        elif 'Monday' in table_day_range and 'Wednesday' in table_day_range:
            route_days = ['MON', 'TUE', 'WED']
        elif 'Thursday' in table_day_range and 'Friday' in table_day_range:
            route_days = ['THU', 'FRI']
        elif 'Monday' in table_day_range and 'Thursday' in table_day_range:
            route_days = ['MON', 'TUE', 'WED', 'THU']
        elif 'Sunday' in table_day_range:
            route_days = ['SUN']
        elif 'Monday' in table_day_range:
            route_days = ['MON']
        elif 'Tuesday' in table_day_range:
            route_days = ['TUE']
        elif 'Wednesday' in table_day_range:
            route_days = ['WED']
        elif 'Thursday' in table_day_range:
            route_days = ['THU']
        elif 'Friday' in table_day_range:
            route_days = ['FRI']
        elif 'Saturday' in table_day_range:
            route_days = ['SAT']

        route_number = table_day_range[6:8].strip()
        route_direction = ''
        if 'Outbound' in table_titles:
            route_direction = 'OUT'
        elif 'Inbound' in table_titles:
            route_direction = 'IN'
        else:
            route_direction = 'LOOP'

        # print route_number
        # print route_days
        # print route_direction
        # print table_stops
        # print table_times

        for stop_index, stop in enumerate(table_stops):
            for time_index, time in enumerate(table_times):
                pass

        new_route_values = []
        for stop_names, stop_times in zip(table_stops, table_times):
            new_route_value = {}
            stop_names_len = len(stop_names)
            for stop_index, new_stop_name in enumerate(stop_names):
                new_stop_times = []
                for time_index, time in enumerate(stop_times):
                    if (time_index % stop_names_len) == stop_index:
                        new_stop_times.append(time)
                new_route_value[new_stop_name] = new_stop_times
            new_route_values.append(new_route_value)

        new_route_names = []
        for i in range(len(new_route_values)):
            for day in route_days:
                route_num = 0
                new_route_name = 'route' + route_number + day + str(route_num)
                while new_route_name in routes:
                    route_num += 1
                    new_route_name = 'route' + route_number + day + str(route_num)
                routes[new_route_name] = {}
                new_route_names.append(new_route_name)

        new_route_values_len = len(new_route_values)
        for new_route_index, new_route_name in enumerate(new_route_names):
            routes[new_route_name] = new_route_values[new_route_index / new_route_values_len]


MON = {}
TUE = {}
WED = {}
THU = {}
FRI = {}
SAT = {}
SUN = {}

for key, value in routes.iteritems():
    if key[7:10] == "MON":
        MON[key] = value 
    elif key[7:10] == "TUE":
        TUE[key] = value
    elif key[7:10] == "WED":
        WED[key] = value
    elif key[7:10] == "THU":
        THU[key] = value
    elif key[7:10] == "FRI":
        FRI[key] = value
    elif key[7:10] == "SAT":
        SAT[key] = value
    elif key[7:10] == "SUN":
        SUN[key] = value
    
route_data = {}
route_data['MON'] = MON
route_data['TUE'] = TUE
route_data['WED'] = WED
route_data['THU'] = THU
route_data['FRI'] = FRI
route_data['SAT'] = SAT
route_data['SUN'] = SUN

print route_data
if len(routes) > 0:
    route_data = str(route_data)
    route_data = route_data.replace("'", '"')
    route_data = route_data.replace('"s', "'")
    open('data/route_data.txt', 'w').write(route_data)










