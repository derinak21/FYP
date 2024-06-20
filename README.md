# FYP

This is the code repository for my Final Year Project at Imperial College London for BEng Electronic and Information Engineering degree. 


Abstract From My Report

With respiratory diseases being one of the leading causes of death, personalized respiratory health management systems at home are needed. This project presents a mobile application integrated with a wearable device to gather biosignals and environmental parameters. The mobile application has two modes: training and monitoring. The training mode assists patients with breathing exercises with the data from a k-RIP sensor by visualizing the ideal breathing patterns for the exercises and the real-time breathing pattern for comparison. The monitoring mode uses biosignals and environmental parameters from the sensors and the internet to provide the risk of an upcoming respiratory exacerbation. The risk calculations are performed using a fuzzy logic model, personalized with patient-specific triggers and resting heart rate. A threshold-based classifier is built on the risk calculations of the fuzzy logic to predict a possible exacerbation. This classifier uses online learning, continuously being optimized by real-time user feedback. 


In this repository, there are 3 folders.

Mobile app folder provides the code for the developed mobile app. A video of the mobile app can be seen in ...

Server folder provides the code for the server and the fuzzy logic script running on AWS EC2 instance. 

Wearables folder includes the code for the garment which measures the breathing patterns using k-RIP sensor. It also provides the code for the smart wristband that has MAX30101 Heart Rate Monitor and Pulse Oximeter sensor, BME 280 Humidity, Pressure and Temperature Sensor and ENS160 Digital Metal-Oxide Multi-Gas Sensor. 