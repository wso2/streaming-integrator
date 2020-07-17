# How to Use Grafana Dashboards

In order to use these dashboards with grafana you have to follow these instructions.

## 1. Configuring Prometheus Reporter.  
To enable statistics for the Prometheus Reporter, add the following configuration to deployment.yaml
1. Navigate to `<WSO2_SI_HOME>/conf/server/deployment.yaml`.
2. Add following Configurations,
    ````
    wso2.metrics:
      # Enable Metrics
      enabled: true
      reporting:
        console:
          - # The name for the Console Reporter
            name: Console
    
            # Enable Console Reporter
            enabled: false
    
            # Polling Period in seconds.
            # This is the period for polling metrics from the metric registry and printing in the console
            pollingPeriod: 2
    
    metrics.prometheus:
     reporting:
       prometheus:
         - name: prometheus
           enabled: true
           serverURL: "http://localhost:9005"
    ````    
3. Then to enable siddhi application level metrics we need to add the `@App:statistics` annotation below 
the Siddhi application name in the Siddhi file as shown in below.
    ````
   @App:name('TestMetrics')
   @App:statistics(reporter = 'prometheus')
   define stream TestStream (message string);
   ````


## 2. Start the Streaming Integrator.
1. Navigate to <WSO2_SI_HOME> and issue following command in the terminal,
    1. `bin/server.sh` (if you are on a Linux/Mac OS) .
    2. `server.bat` (if you are on a Windows OS).

## 3. Start the Prometheus Server.
1. Install the Prometheus Server using the following link.
   https://prometheus.io/docs/prometheus/latest/getting_started/
2. Navigate to prometheus directory and open the `prometheus.yaml` file  
3. Add following configurations under the `scrape_configs:`  
    ````
    scrape_configs:
      - job_name: 'wso2'
        static_configs:
        - targets: ['localhost:9005']
    ````  
4. Start the prometheus server by executing following command in the terminal `./prometheus`

## 4. Start & Configure the Grafana Server.
1. Download Grafana from the following URL  https://grafana.com/grafana/download  
2. Extract the downloaded file and Navigate to grafana directory.  
3. Issue following command in the console `bin/grafana-server`.  
4. Navigate to grafana home with following URL `localhost:3000`.
5. Then navigate `Create your first data source` and select `Prometheus`.
6. Then update the configurations as shown in below,    
![](./image-2.jpg)


## 5. Load dashboards into Grafana.
Once you have login to the Grafana follow these steps to import dashboards into Grafana

1. Navigate to +(plus) icon at the left upper corner.  
![](./image-1.png)

2. Select `import`.  
3. Then select `upload .json file` and select relevant json file from this directory.

