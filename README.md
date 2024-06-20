# Final Year Project Repository

This is the code repository for my Final Year Project at Imperial College London for the BEng Electronic and Information Engineering degree.

## Abstract From My Report

With respiratory diseases being one of the leading causes of death, personalized respiratory health management systems at home are needed. This project presents a mobile application integrated with a wearable device to gather biosignals and environmental parameters. The mobile application has two modes: training and monitoring. The training mode assists patients with breathing exercises with the data from a k-RIP sensor by visualizing the ideal breathing patterns for the exercises and the real-time breathing pattern for comparison. The monitoring mode uses biosignals and environmental parameters from the sensors and the internet to provide the risk of an upcoming respiratory exacerbation. The risk calculations are performed using a fuzzy logic model, personalized with patient-specific triggers and resting heart rate. A threshold-based classifier is built on the risk calculations of the fuzzy logic to predict a possible exacerbation. This classifier uses online learning, continuously being optimized by real-time user feedback.

## Repository Structure

This repository contains three main folders:

1. **[Mobile App](./MobileApp)**
    - Provides the code for the developed mobile app.
  
2. **[Server](./Server)**
    - Contains the code for the server and the fuzzy logic script running on an AWS EC2 instance.

3. **[Wearables](./Wearables)**
    - Includes the code for the garment that measures breathing patterns using the k-RIP sensor.
    - Provides the code for the smart wristband equipped with:
        - MAX30101 Heart Rate Monitor and Pulse Oximeter sensor
        - BME 280 Humidity, Pressure, and Temperature Sensor
        - ENS160 Digital Metal-Oxide Multi-Gas Sensor

## Demonstration

The mobile app is demonstrated in videos available at the following links:
- [Account Activities](https://imperiallondon-my.sharepoint.com/:v:/g/personal/da621_ic_ac_uk/ERtdLEXPl4dNl51ot5tbwtoBEjh0DHPVnYBs0em9LBKt3w?nav=eyJyZWZlcnJhbEluZm8iOnsicmVmZXJyYWxBcHAiOiJPbmVEcml2ZUZvckJ1c2luZXNzIiwicmVmZXJyYWxBcHBQbGF0Zm9ybSI6IldlYiIsInJlZmVycmFsTW9kZSI6InZpZXciLCJyZWZlcnJhbFZpZXciOiJNeUZpbGVzTGlua0NvcHkifX0&e=tFDLUH)
- [Breathing Exercises (Only App Shown)](https://imperiallondon-my.sharepoint.com/:v:/g/personal/da621_ic_ac_uk/EW1cxNTBPCJHq4XQZLuYRyoBkxLemFYHq6hwL--pIx9G1w?nav=eyJyZWZlcnJhbEluZm8iOnsicmVmZXJyYWxBcHAiOiJPbmVEcml2ZUZvckJ1c2luZXNzIiwicmVmZXJyYWxBcHBQbGF0Zm9ybSI6IldlYiIsInJlZmVycmFsTW9kZSI6InZpZXciLCJyZWZlcnJhbFZpZXciOiJNeUZpbGVzTGlua0NvcHkifX0&e=UFx3tP)
- [Breathing Exercises (App and Chest Phantom Shown)](https://imperiallondon-my.sharepoint.com/:v:/g/personal/da621_ic_ac_uk/EYGV2KE7P0VPpEJfpAZv4NYBhwXwIel0a3YsGLkS7C2tTg?nav=eyJyZWZlcnJhbEluZm8iOnsicmVmZXJyYWxBcHAiOiJPbmVEcml2ZUZvckJ1c2luZXNzIiwicmVmZXJyYWxBcHBQbGF0Zm9ybSI6IldlYiIsInJlZmVycmFsTW9kZSI6InZpZXciLCJyZWZlcnJhbFZpZXciOiJNeUZpbGVzTGlua0NvcHkifX0&e=74hVqS)
- [Monitoring](https://imperiallondon-my.sharepoint.com/:v:/g/personal/da621_ic_ac_uk/Eb3CqKsqM8JMsBQLeJUWD7YBHIh6Y7cl_0o3WRcqYHry6Q?nav=eyJyZWZlcnJhbEluZm8iOnsicmVmZXJyYWxBcHAiOiJPbmVEcml2ZUZvckJ1c2luZXNzIiwicmVmZXJyYWxBcHBQbGF0Zm9ybSI6IldlYiIsInJlZmVycmFsTW9kZSI6InZpZXciLCJyZWZlcnJhbFZpZXciOiJNeUZpbGVzTGlua0NvcHkifX0&e=BERilG)
