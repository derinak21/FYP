import numpy as np
import skfuzzy as fuzz
from skfuzzy import control as ctrl
import matplotlib.pyplot as plt
import paho.mqtt.client as mqtt
import json
import openmeteo_requests
from datetime import datetime
import requests_cache
import pandas as pd
from retry_requests import retry


cache_session = requests_cache.CachedSession('.cache', expire_after=3600)
retry_session = retry(cache_session, retries=5, backoff_factor=0.2)
openmeteo = openmeteo_requests.Client(session=retry_session)
threshold = 70
learning_rate = 0.1
in_out_status = "in"
user_fuzzy_systems = {}


database = {}

# Define the fuzzy logic system
def create_fuzzy_system(restingheartrate, trigger):
    # Heart Rate
    hr = ctrl.Antecedent(np.arange(30, 250, 1), "HR")
    hr['normal'] = fuzz.zmf(np.arange(30, 250, 1), restingheartrate, 110)
    hr['high'] = fuzz.smf(np.arange(30, 250, 1), restingheartrate, 110)

    # Respiration Rate
    rr = ctrl.Antecedent(np.arange(0, 60, 1), "RR")
    rr['normal'] = fuzz.zmf(np.arange(0, 60, 1), 15, 25)
    rr['high'] = fuzz.smf(np.arange(00, 60, 1), 15, 25)

    # Temperature
    temp = ctrl.Antecedent(np.arange(-10, 80, 1), "Temperature")
    temp["low"] = fuzz.trapmf(temp.universe, [-10, -10, 0, 21])
    temp["ideal"] = fuzz.trimf(temp.universe, [0, 21, 32])
    temp["high"] = fuzz.trapmf(temp.universe, [21, 32, 50, 50])

    # Humidity
    hum = ctrl.Antecedent(np.arange(0, 100, 1), "Humidity")
    hum["low"] = fuzz.trapmf(hum.universe, [0, 0, 30, 40])
    hum["ideal"] = fuzz.trimf(hum.universe, [30, 40, 50])
    hum["high"] = fuzz.trapmf(hum.universe, [40, 50, 100, 100])



    # AQI
    aqi1 = ctrl.Antecedent(np.arange(1, 5, 1), "aqi1")
    aqi1["low"] = fuzz.trapmf(aqi1.universe, [1, 1, 2, 4])
    aqi1["high"] = fuzz.trapmf(aqi1.universe, [2, 4, 5, 5])


    # AQI2
    aqi2 = ctrl.Antecedent(np.arange(1, 5, 1), "aqi2")
    aqi2["low"] = fuzz.trapmf(aqi2.universe, [0, 0, 40, 100])
    aqi2["high"] = fuzz.trapmf(aqi2.universe, [40, 100, 1200, 1200])

    # pm2.5
    pm2_5 = ctrl.Antecedent(np.arange(0, 800, 1), "PM2.5")
    pm2_5["low"] = fuzz.trapmf(pm2_5.universe, [0, 0, 12, 35])
    pm2_5["high"] = fuzz.trapmf(pm2_5.universe, [12, 35, 800, 800])

    # pm10
    pm10 = ctrl.Antecedent(np.arange(0, 1200, 1), "PM10")
    pm10["low"] = fuzz.trapmf(pm10.universe, [0, 0, 55, 155])
    pm10["high"] = fuzz.trapmf(pm10.universe, [55, 155, 155, 1200])


    # nitrogen_dioxide
    nitrogen_dioxide = ctrl.Antecedent(np.arange(0, 1000, 1), "Nitrogen Dioxide")
    nitrogen_dioxide["low"] = fuzz.trapmf(nitrogen_dioxide.universe, [0, 0, 101, 188])
    nitrogen_dioxide["high"] = fuzz.trapmf(nitrogen_dioxide.universe, [101, 188, 1000, 1000])


    # sulfur_dioxide
    sulfur_dioxide = ctrl.Antecedent(np.arange(0, 1250, 1), "Sulfur Dioxide")
    sulfur_dioxide["low"] = fuzz.trapmf(sulfur_dioxide.universe, [0, 0, 35, 196])
    sulfur_dioxide["high"] = fuzz.trapmf(sulfur_dioxide.universe, [35, 196, 1250, 1250])




    # Define output
    likelihood = ctrl.Consequent(np.arange(0, 101, 1), "likelihood")
    likelihood['low'] = fuzz.trapmf(likelihood.universe, [0, 0, 25, 40])
    likelihood['medium'] = fuzz.trimf(likelihood.universe, [30, 50, 70])
    likelihood['high'] = fuzz.trapmf(likelihood.universe, [60, 90, 100, 100])

    rule1 = ctrl.Rule(hr['normal'] & rr['normal'] & aqi1['low'] & aqi2['low'], likelihood['low'])
    rule2 = ctrl.Rule(hr['high'] | rr['high'], likelihood['high'])
    rule3 = ctrl.Rule((aqi1['high'] | aqi2['high']) & hr['normal'] & rr['normal'], likelihood['medium'])


    low_conditions_indoor = hr['normal'] & rr['normal']
    medium_conditions_indoor = aqi1['high'] 
    low_conditions = hr['normal'] & rr['normal']
    medium_conditions = aqi1['high'] | aqi2['high']
    high_conditions = hr['high'] | rr['high']
    super_high_conditions = hr['high'] | rr['high']

    if "Temperature" in trigger:
        medium_conditions |= temp['high']
        medium_conditions |= temp['low']
        low_conditions &= temp['ideal']
        medium_conditions_indoor |= temp['high']
        medium_conditions_indoor |= temp['low']
        low_conditions_indoor &= temp['ideal']

    if "Humidity" in trigger:
        medium_conditions |= hum['low']
        medium_conditions |= hum['high']
        low_conditions &= hum['ideal']
        medium_conditions_indoor |= hum['low']
        medium_conditions_indoor |= hum['high']
        low_conditions_indoor &= hum['ideal']

 

    if "PM2.5" in trigger:
        medium_conditions |= pm2_5['high']
        low_conditions &= pm2_5['low']

    if "PM10" in trigger:
        medium_conditions |= pm10['high']
        low_conditions &= pm10['low']

    if "Sulfur Dioxide" in trigger:
        medium_conditions |= sulfur_dioxide['high']
        low_conditions &= sulfur_dioxide['low']

    if "Nitrogen Dioxide" in trigger:
        medium_conditions |= nitrogen_dioxide['high']
        low_conditions &= nitrogen_dioxide['low']



    combined_rule_low = ctrl.Rule(low_conditions, likelihood['low'])
    combined_rule_medium = ctrl.Rule(medium_conditions & hr['normal'] & rr['normal'], likelihood['medium'])
    combined_rule_high = ctrl.Rule(high_conditions, likelihood['high'])
    combined_rule_low_indoor = ctrl.Rule(low_conditions_indoor, likelihood['low'])
    combined_rule_medium_indoor = ctrl.Rule(medium_conditions_indoor & hr['normal'] & rr['normal'], likelihood['medium'])

    likelihood_ctrl = ctrl.ControlSystem([combined_rule_low, combined_rule_medium, combined_rule_high])
    indoor = ctrl.ControlSystem([combined_rule_low_indoor, combined_rule_medium_indoor, combined_rule_high])
    outdoor = ctrl.ControlSystem([combined_rule_low, combined_rule_medium, combined_rule_high])

    likelihood_calc = ctrl.ControlSystemSimulation(likelihood_ctrl)
    outdoor_likelihood_calc = ctrl.ControlSystemSimulation(outdoor)
    indoor_likelihood_calc = ctrl.ControlSystemSimulation(indoor)

    return indoor_likelihood_calc, outdoor_likelihood_calc




def update_likelihood(likelihood_calc, triggers, values):
    
    likelihood_calc.input['RR'] = values['RR']
    likelihood_calc.input['HR'] = values['HR']
    likelihood_calc.input['aqi1'] = values['aqi1']

    if values['in_out']== 'out':
        likelihood_calc.input['aqi2'] = values['aqi2']
        for trigger in triggers:
            likelihood_calc.input[trigger] = values[trigger]

    elif values['in_out']== 'in':
        for trigger in triggers:
            if trigger in ('Temperature', 'Humidity', 'Volatile Organic Compounds'):
                likelihood_calc.input[trigger] = values[trigger]

    likelihood_calc.compute()
    return likelihood_calc.output['likelihood']

def fetch_outdoor_conditions(latitude, longitude):
    url = "https://air-quality-api.open-meteo.com/v1/air-quality"
    params = {
        "latitude": latitude,
        "longitude": longitude,
        "current": ["european_aqi", "pm10", "pm2_5", "carbon_monoxide", "nitrogen_dioxide", "sulphur_dioxide", "ozone"],
        "hourly": ["pm10", "pm2_5"]
    }
    responses = openmeteo.weather_api(url, params=params)

    # Process first location. Add a for-loop for multiple locations or weather models
    response = responses[0]
    # print(f"Coordinates {response.Latitude()}°N {response.Longitude()}°E")
    # print(f"Elevation {response.Elevation()} m asl")
    # print(f"Timezone {response.Timezone()} {response.TimezoneAbbreviation()}")
    # print(f"Timezone difference to GMT+0 {response.UtcOffsetSeconds()} s")

    # Current values. The order of variables needs to be the same as requested.
    current = response.Current()
    current_european_aqi = current.Variables(0).Value()
    current_pm10 = current.Variables(1).Value()
    current_pm2_5 = current.Variables(2).Value()
    current_carbon_monoxide = current.Variables(3).Value()
    current_nitrogen_dioxide = current.Variables(4).Value()
    current_sulphur_dioxide = current.Variables(5).Value()
    current_ozone = current.Variables(6).Value()

    # print(f"Current time {current.Time()}")
    # print(f"Current european_aqi {current_european_aqi}")
    # print(f"Current pm10 {current_pm10}")
    # print(f"Current pm2_5 {current_pm2_5}")
    # print(f"Current carbon_monoxide {current_carbon_monoxide}")
    # print(f"Current nitrogen_dioxide {current_nitrogen_dioxide}")
    # print(f"Current sulphur_dioxide {current_sulphur_dioxide}")
    # print(f"Current ozone {current_ozone}")

    # Process hourly data. The order of variables needs to be the same as requested.
    hourly = response.Hourly()
    hourly_pm10 = hourly.Variables(0).ValuesAsNumpy()
    hourly_pm2_5 = hourly.Variables(1).ValuesAsNumpy()

    hourly_data = {"date": pd.date_range(
        start = pd.to_datetime(hourly.Time(), unit = "s", utc = True),
        end = pd.to_datetime(hourly.TimeEnd(), unit = "s", utc = True),
        freq = pd.Timedelta(seconds = hourly.Interval()),
        inclusive = "left"
    )}
    hourly_data["pm10"] = hourly_pm10
    hourly_data["pm2_5"] = hourly_pm2_5

    hourly_dataframe = pd.DataFrame(data = hourly_data)

    current_datetime = datetime.now()
    today_data = hourly_dataframe[hourly_dataframe['date'].dt.date == current_datetime.date()]
    current_hour_data = today_data[today_data['date'].dt.hour == current_datetime.hour]


    pm10_value = current_hour_data['pm10'].iloc[0]
    pm2_5_value = current_hour_data['pm2_5'].iloc[0]

    print("PM10 value:", pm10_value)
    print("PM2.5 value:", pm2_5_value)

    return current_european_aqi, current_pm2_5, current_pm10, current_ozone, current_nitrogen_dioxide, current_sulphur_dioxide, current_carbon_monoxide


def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))
    client.subscribe("monitor")
    client.subscribe("in_out")
    client.subscribe("location")
    client.subscribe("attackfeedback")
    client.subscribe("user-triggers")
    client.subscribe("sensordata_wrist")
    client.subscribe("iot")


def on_message(client, userdata, msg):
    global in_out_status
    global database
    global threshold
    global learning_rate
    print(f"Message received on topic {msg.topic}: {msg.payload}")
    print("database: ", database)

    payload = msg.payload.decode("utf-8")
    
    if msg.topic == "in_out":
        deviceid, in_out = payload.split(',')
        database[deviceid]['in_out']= in_out
        
    elif msg.topic == "user-triggers":
        print(payload)
        a = []
        a = payload.split(',')
        deviceid = a[0]
        restingheartrate = float(a[1])
        triggers = a[2:]
        print("deviceid: ", deviceid)
        print("restingheartrate: ", restingheartrate)
        print("triggers: ", triggers)

        if deviceid not in database:
            database[deviceid] = {}

        if deviceid not in user_fuzzy_systems:
            user_fuzzy_systems[deviceid] = {}

        database[deviceid]['triggers'] = triggers
        print("triggers in database: ", database)
        database[deviceid]['aqi1']= "null"
        database[deviceid]['HR']= "null"
        database[deviceid]['RR']= "null"
        database[deviceid]['aqi2']= "null"
        database[deviceid]['restingheartrate']= restingheartrate
        database[deviceid]['risk']= 0
        database[deviceid]['threshold']= 70

       
        for trigger in triggers:
            database[deviceid][trigger] = "null"
        if user_fuzzy_systems[deviceid]=={}:
            user_fuzzy_systems[deviceid]['indoor'], user_fuzzy_systems[deviceid]['outdoor'] = create_fuzzy_system(restingheartrate, triggers)
            print(user_fuzzy_systems[deviceid])

    elif msg.topic == "attackfeedback":
        print("attackfeedback")
        print(payload)
        deviceid, result = payload.split(",")
        if result== "no":
            database[deviceid]['threshold'] = threshold + learning_rate* (database[deviceid]['risk']-database[deviceid]['threshold'])
            print("new threshold: ", database[deviceid]['threshold'])

    elif msg.topic == "location":
        print("location received: ", payload)
        deviceid, latitude, longitude = payload.split(',')
        latitude = float(latitude)
        longitude = float(longitude)
       

        if "in_out" not in database[deviceid]:
            database[deviceid]['in_out'] = "out"

        current_european_aqi, pm2_5_value, pm10_value, current_ozone, current_nitrogen_dioxide, current_sulphur_dioxide, current_carbon_monoxide = fetch_outdoor_conditions(latitude, longitude)
        database[deviceid]['aqi2'] = current_european_aqi
        database[deviceid]['PM2.5'] = pm2_5_value
        database[deviceid]['PM10'] = pm10_value
        database[deviceid]['Nitrogen Dioxide'] = current_nitrogen_dioxide
        database[deviceid]['Sulfur Dioxide'] = current_sulphur_dioxide
        database[deviceid]['Carbon Monoxide'] = current_carbon_monoxide
        database[deviceid]['RR'] = 15

        donotupdate= False
        for key, value in database[deviceid].items():
            if value== 'null':
                donotupdate = True
        if not donotupdate:
            if database[deviceid]['in_out']=='out':
                likelihood_result= update_likelihood(user_fuzzy_systems[deviceid]['outdoor'],database[deviceid]['triggers'], database[deviceid])
            else:
                likelihood_result = update_likelihood(user_fuzzy_systems[deviceid]['indoor'],database[deviceid]['triggers'], database[deviceid])
            print("Classifier:", likelihood_result>threshold)
            database[deviceid]['risk'] = likelihood_result
            client.publish("fuzzyfeedback", f"{likelihood_result},{likelihood_result>database[deviceid]['threshold']}")
        else:
            print('got outside values but not enough data yet')

        client.publish("air_pollution", f"{round(current_european_aqi, 2)},{round(pm2_5_value, 2)},{round(pm10_value, 2)},{round(current_ozone, 2)},{round(current_nitrogen_dioxide, 2)},{round(current_sulphur_dioxide, 2)},{round(current_carbon_monoxide, 2)}")
            

    elif msg.topic == "sensordata_wrist":
        deviceid, temp, hum, aqi, tvoc, co2, bpm = payload.split(',')
        temp = float(temp)
        hum = float(hum)
        aqi = float(aqi)
        tvoc = float(tvoc)
        co2 = float(co2)
        bpm = float(bpm)
        if deviceid not in database:
            database[deviceid] = {}
        if "in_out" not in database[deviceid]:
            database[deviceid]['in_out'] = "out"
        database[deviceid]['Temperature'] = temp
        database[deviceid]['Humidity'] = hum
        database[deviceid]['aqi1'] = aqi
        database[deviceid]['Volatile Organic Compounds'] = tvoc
        database[deviceid]['HR'] = bpm

        donotupdate= False
        for key, value in database[deviceid].items():
            if value== 'null':
                donotupdate = True
                print(database[deviceid])
        print(database[deviceid]['triggers'])
        if not donotupdate:
            print("database: ", database)
            if database[deviceid]['in_out']=='out':
                likelihood_result = update_likelihood(user_fuzzy_systems[deviceid]['outdoor'],database[deviceid]['triggers'], database[deviceid])
            else:
                likelihood_result = update_likelihood(user_fuzzy_systems[deviceid]['indoor'],database[deviceid]['triggers'], database[deviceid])
            print("Likelihood of asthma/COPD attack:", likelihood_result)
            print("Classifier:", likelihood_result>threshold)
            database[deviceid]['risk'] = likelihood_result
            client.publish("fuzzyfeedback", f"{likelihood_result},{likelihood_result>database[deviceid]['threshold']}")
        else:
            print('got wrist values but not enough data yet')

    elif msg.topic == "iot":
        deviceid, rr = payload.split(',')
        rr = float(rr)
    
        if deviceid not in database:
            database[deviceid] = {}
        if "in_out" not in database[deviceid]:
            database[deviceid]['in_out'] = "out"
        database[deviceid]['RR'] = rr
        donotupdate= False
        for key, value in database[deviceid].items():
            if value== 'null':
                donotupdate = True
        if not donotupdate:
            if database[deviceid]['in_out']=='out':
                likelihood_result = update_likelihood(user_fuzzy_systems[deviceid]['outdoor'],database[deviceid]['triggers'], database[deviceid])
            else:
                likelihood_result = update_likelihood(user_fuzzy_systems[deviceid]['indoor'],database[deviceid]['triggers'], database[deviceid])
            print("Likelihood of asthma/COPD attack:", likelihood_result)
            print("Classifier:", likelihood_result>threshold)
            database[deviceid]['risk'] = likelihood_result
            client.publish("fuzzyfeedback", f"{likelihood_result},{likelihood_result>database[deviceid]['threshold']}")





# Set up the MQTT client
client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message


client.connect("18.169.68.55", 1883)

client.loop_start()



try:
    while True:
        pass
except KeyboardInterrupt:
    print("Disconnecting from broker")
    client.disconnect()
    client.loop_stop()
