import urllib2
import re
import sys
from time import sleep
from pykml import parser
from pyquery import PyQuery as pq

# some bus stops have inconsistent names across data sources
# we deal with that here
juice_list = {'Goldwin Smith Hall': 'Goldwin Smith'}
def replace_juice_listed(juiced):
    for k, v in juice_list.iteritems():
        juiced[v] = juiced[k]
        del juiced[k]
    return juiced

def replace_blaze(juiced):
    juiced = juiced.replace('"', 'kj324h5249fdj')
    juiced = juiced.replace("'", '"')
    juiced = juiced.replace('"s', "'s")
    juiced = juiced.replace('s" ', "s' ")
    juiced = juiced.replace('kj324h5249fdj', "'")
    juiced = juiced.replace(", '", ', "')
    juiced = juiced.replace("', ", '", ')
    juiced = juiced.replace("['", '["')
    juiced = juiced.replace("']", '"]')
    juiced = juiced.replace("':", '":')
    return juiced

def transform_coordinate_triplets(coordinate_list):
    return [[coordinate_list[i], coordinate_list[i + 2], coordinate_list[i + 1]] for i in xrange(0, len(coordinate_list), 3)]

def transform_coordinate_quartets(coordinate_list):
    return list((coordinate_list[i:i + 3][::-1] for i in xrange(0, len(coordinate_list), 3)))

# this creates a file containing a list of [lat, lng] pairs that traces each route
# these are the routes we will collect data for
# routes = [10, 11, 13, 14, 15, 17, 20, 21, 30, 31, 32, 36, 37, 40, 41, 43, 51, 52, 53, 65, 67, 70, 72, 75, 77, 81, 82, 83, 90, 92, 93]
routes = []
for route in routes:
    data = urllib2.urlopen('http://www.tcatbus.com/kml/route' + str(route) + '.kml').read();
    # data = open('data.txt').read()
    try:
        root = parser.fromstring(data)
        coordinate_data = str(root.Document.Placemark.LineString.coordinates).strip()
        coordinate_data = re.compile('[,\s]').split(coordinate_data)
        coordinate_list = transform_coordinate_triplets(coordinate_data)
        coordinate_list = str(coordinate_list)
        coordinate_list = coordinate_list.replace("'", '"')
        open('data/route' + str(route) + '.txt', 'w').write(coordinate_list)
    except:
        print 'Failed to parse route ', route
    # sleep so the server doesn't get suspicious and kick us out
    sleep(0.5)

# this creates a dict like {stop_name: [lat, lng]} and a list like [[stop, lat, lng]]
# these are the routes we will collect data for
# stops = ["Cornell", "Downtown", "Country"]
stops = []
full_coordinate_list = []
full_stop_list = []
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
    coordinate_data = coordinate_data.replace(',0', '')
    coordinate_data = coordinate_data.split(',')

    # for full_coordinate_list
    coordinate_list = transform_coordinate_quartets(coordinate_data)
    full_coordinate_list.extend(coordinate_list)

    # for full_stop_list
    stop_list = transform_coordinate_triplets(coordinate_data)
    full_stop_list.extend(stop_list)
    # sleep so the server doesn't get suspicious and kick us out
    sleep(1)
coordinate_dictionary = {}
for coordinates in full_coordinate_list:
    coordinate_dictionary[coordinates[2]] = coordinates[:2]
if len(full_coordinate_list) > 0:
    coordinate_dictionary = str(coordinate_dictionary)
    coordinate_dictionary = replace_blaze(coordinate_dictionary)
    open('data/stops_dictionary.txt', 'w').write(coordinate_dictionary)
    full_stop_list = str(full_stop_list)
    full_stop_list = replace_blaze(full_stop_list)
    open('data/stops_list.txt', 'w').write(full_stop_list)

# this creates a dictionary like {route_name: tcat_route_id}
should_generate_new_route_dictionary = True
if should_generate_new_route_dictionary:
    stop_to_id_dictionary = {}
    data = urllib2.urlopen('http://tcat.nextinsight.com/').read();
    data = data.replace('---', '--')
    d = pq(data)
    select_element = d('#start')
    option_elements = select_element('option')
    for i in option_elements:
        stop_to_id_dictionary[i.text_content()[1:]] = i.get('value')
    stop_to_id_dictionary = replace_juice_listed(stop_to_id_dictionary)
    stop_to_id_dictionary = str(stop_to_id_dictionary)
    stop_to_id_dictionary = replace_blaze(stop_to_id_dictionary)
    open('data/stop_to_id_dictionary.txt', 'w').write(stop_to_id_dictionary)






























# # a dictionary where each entry is (e.g.) {"route11MON" + n where n is some integer: value}
# # the values are dictionaries where each entry is (e.g.) {"stop name": [times]}
# routes = {}
# # these are the numbers for each route to query on tcat's website
# # route_schedules = ['343', '415', '416', '428', '453', '313', '314', '315', '386', '482', '424', '389', '484', '391', '340', '393', '460', '329', '330', '401', '422', '478', '320', '325', '406', '407', '408', '398', '430', '410', '426', '427']
# route_schedules = []
# if len(route_schedules) > 0:
#     open('schedule_test_data.txt', 'w').write('')
# for route_schedule in route_schedules:
#     data = urllib2.urlopen('http://tcat.nextinsight.com/routes/' + route_schedule).read()
#     open('schedule_test_data.txt', 'a').write(data)
#     # data = open('schedule_test_data.txt').read()
#     data = data.replace('---', '--')

#     # parse the data and put it into a pyquery object
#     d = pq(data)
#     tables = d('table').filter(lambda i: i > 1)
#     # for each table of schedules (different outbound/inbound & day of week of routes for each table)
#     for index, table in enumerate(tables.items()):
#         # an array containing each stop td element in the table
#         stops = list(table.children('tr').filter(lambda i: i == 1).children('td'))

#         # a matrix representing the time td elements in the table
#         times_matrix = []
#         for j in range(4, len(table.children('tr'))):
#             times_matrix.append(list(table.children('tr').filter(lambda i: i == j).children('td')))

#         table_stops = []
#         table_times = []
#         for time_index, times in enumerate(times_matrix):
#             potential_stops_to_add = []
#             potential_times_to_add = []
#             for index, time_td in enumerate(times):
#                 if str(time_td.text_content()) != '--' and index < len(stops):
#                     potential_stops_to_add.append(stops[index].text_content())
#                     potential_times_to_add.append(time_td.text_content())
#             if not (potential_stops_to_add in table_stops):
#                 table_stops.append([])
#                 table_times.append([])
#                 table_stops[len(table_stops) - 1].extend(potential_stops_to_add)
#                 table_times[len(table_times) - 1].extend(potential_times_to_add)
#             else:
#                 table_times[table_stops.index(potential_stops_to_add)].extend(potential_times_to_add)

#         # this will be something like 'Outbound/Inbound from the center of Ithaca' or 'Loop'
#         table_titles = list(table.children('tr').children('td').children('h6'))[0].text_content()

#         # this will be something like 'Route 11 Monday - Wednesday'
#         table_day_range = list(table.children('h5'))[0].text_content()
        
#         # parse the days from the date range
#         route_days = []
#         if 'Weekday' in table_day_range or ('Monday' in table_day_range and 'Friday' in table_day_range):
#             route_days = ['MON', 'TUE', 'WED', 'THU', 'FRI']
#         elif 'Weekend' in table_day_range or ('Saturday' in table_day_range and 'Sunday' in table_day_range):
#             route_days = ['SUN', 'SAT']
#         elif 'Monday' in table_day_range and 'Wednesday' in table_day_range:
#             route_days = ['MON', 'TUE', 'WED']
#         elif 'Thursday' in table_day_range and 'Friday' in table_day_range:
#             route_days = ['THU', 'FRI']
#         elif 'Monday' in table_day_range and 'Thursday' in table_day_range:
#             route_days = ['MON', 'TUE', 'WED', 'THU']
#         elif 'Sunday' in table_day_range:
#             route_days = ['SUN']
#         elif 'Monday' in table_day_range:
#             route_days = ['MON']
#         elif 'Tuesday' in table_day_range:
#             route_days = ['TUE']
#         elif 'Wednesday' in table_day_range:
#             route_days = ['WED']
#         elif 'Thursday' in table_day_range:
#             route_days = ['THU']
#         elif 'Friday' in table_day_range:
#             route_days = ['FRI']
#         elif 'Saturday' in table_day_range:
#             route_days = ['SAT']

#         route_number = table_day_range[6:8].strip()
#         route_direction = ''
#         if 'Outbound' in table_titles:
#             route_direction = 'OUT'
#         elif 'Inbound' in table_titles:
#             route_direction = 'IN'
#         else:
#             route_direction = 'LOOP'

#         new_route_values = []
#         for stop_names, stop_times in zip(table_stops, table_times):
#             new_route_value = {}
#             stop_names_len = len(stop_names)
#             for stop_index, new_stop_name in enumerate(stop_names):
#                 new_stop_times = []
#                 for time_index, time in enumerate(stop_times):
#                     if (time_index % stop_names_len) == stop_index:
#                         new_stop_times.append(time)
#                 new_route_value[new_stop_name] = new_stop_times
#             new_route_values.append(new_route_value)

#         new_route_names = []
#         for i in range(len(new_route_values)):
#             for day in route_days:
#                 route_num = 0
#                 new_route_name = 'route' + route_number + day + str(route_num)
#                 while new_route_name in routes:
#                     route_num += 1
#                     new_route_name = 'route' + route_number + day + str(route_num)
#                 routes[new_route_name] = {}
#                 new_route_names.append(new_route_name)

#         new_route_values_len = len(new_route_values)
#         for new_route_index, new_route_name in enumerate(new_route_names):
#             routes[new_route_name] = new_route_values[new_route_index / len(route_days)]
#     sleep(0.5)


# MON = {}
# TUE = {}
# WED = {}
# THU = {}
# FRI = {}
# SAT = {}
# SUN = {}

# for key, value in routes.iteritems():
#     if key[7:10] == "MON":
#         MON[key] = value 
#     elif key[7:10] == "TUE":
#         TUE[key] = value
#     elif key[7:10] == "WED":
#         WED[key] = value
#     elif key[7:10] == "THU":
#         THU[key] = value
#     elif key[7:10] == "FRI":
#         FRI[key] = value
#     elif key[7:10] == "SAT":
#         SAT[key] = value
#     elif key[7:10] == "SUN":
#         SUN[key] = value
    
# route_data = {}
# route_data['MON'] = MON
# route_data['TUE'] = TUE
# route_data['WED'] = WED
# route_data['THU'] = THU
# route_data['FRI'] = FRI
# route_data['SAT'] = SAT
# route_data['SUN'] = SUN

# if len(routes) > 0:
#     route_data = str(route_data)
#     route_data = route_data.replace("'", '"')
#     route_data = route_data.replace('"s', "'")
#     open('data/route_data.txt', 'w').write(route_data)










