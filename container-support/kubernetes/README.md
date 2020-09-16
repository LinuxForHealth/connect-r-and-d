# Linux for Health Kubernetes

LFH can be deployed to a kubernetes cluster using lfh-all.yaml. 

From the "connect" project folder, run: 
``kubectl apply -f container-support/kubernetes/lfh-all.yaml``

The following ports are configured by default on the target cluster:
* 30000: Kafdrop
* 31000: LFH Http port
* 32000: HL7 port
