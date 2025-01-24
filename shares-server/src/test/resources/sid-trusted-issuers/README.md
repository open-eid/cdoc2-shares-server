# Server configuration with trusted certificates

CA certificates can be downloaded and added to trust store according to 
[SK-EID documentation](https://github.com/SK-EID/PKI/wiki/Certification-Hierarchy#what-will-change-in-ca-hierarchy).

## Demo environment certificates
* sid_demo_sk_ee.pem.crt
* tsp_demo_sk_ee_2025.pem.cer
* TEST_EID_NQ_2021E.pem.crt
* TEST_EID_NQ_2021R.pem.crt
* TEST_EID_Q_2021E.pem.crt
* TEST_EID_Q_2021R.pem.crt
* TEST_of_SK_ID_Solutions_EID_Q_2024E.pem.crt
* TEST_of_SK_ID_Solutions_EID_Q_2024R.pem.crt
* TEST_SK_ROOT_G1_2021E.pem.crt
* TEST_SK_ROOT_G1_2021R.pem.crt
* TEST_of_EID_SK_2016.pem.crt
* TEST_of_ESTEID_SK_2015.pem.crt
* TEST_of_NQ_SK_2016.pem.crt
* TEST_of_EE_Certification_Centre_Root_CA.pem.crt
* sk-ca.localhost.crt (mock data certificate for testing purpose)

## Production environment certificates
* EID_NQ_2021E.pem.crt
* EID_NQ_2021R.pem.crt
* EID_Q_2021E.pem.crt
* EID_Q_2021R.pem.crt
* EID_Q_2024E.pem.crt
* EID_Q_2024R.pem.crt
* EID_ROOT_G1_2021E.pem.crt
* EID_ROOT_G1_2021R.pem.crt
* EID_SK_2016.pem.crt
* ESTEID_SK_2015.pem.crt
* NQ_SK_2016.pem.crt
* EE_Certification_Centre_Root_CA.pem.crt

## Script
Run script to create trusted store and add required certificates to it:

```bash
cd shares-server/src/test/resources/sid-trusted-issuers
sh create-truststore_with_certs.sh
```
