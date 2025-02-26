#!/bin/bash


# Generate server key store and import certificates into it
wget https://c.sk.ee/sid_demo_sk_ee.pem.crt
keytool -import -noprompt -v -trustcacerts -keystore test_sid_trusted_issuers.jks \
 -storetype JKS -storepass changeit -alias sid-demo-sk-ee -file sid_demo_sk_ee.pem.crt
rm sid_demo_sk_ee.pem.crt

wget https://c.sk.ee/tsp_demo_sk_ee_2025.pem.cer
keytool -import -noprompt -v -trustcacerts -file tsp_demo_sk_ee_2025.pem.cer \
 -keystore test_sid_trusted_issuers.jks -alias tsp-demo-sk-ee-2025 -storepass changeit
rm tsp_demo_sk_ee_2025.pem.cer

wget https://c.sk.ee/EID_NQ_2021E.pem.crt
keytool -import -noprompt -v -trustcacerts -file EID_NQ_2021E.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias EID-NQ-2021E -storepass changeit
rm EID_NQ_2021E.pem.crt

wget https://c.sk.ee/EID_NQ_2021R.pem.crt
keytool -import -noprompt -v -trustcacerts -file EID_NQ_2021R.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias EID-NQ-2021R -storepass changeit
rm EID_NQ_2021R.pem.crt

wget https://c.sk.ee/EID_Q_2021E.pem.crt
keytool -import -noprompt -v -trustcacerts -file EID_Q_2021E.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias EID-Q-2021E -storepass changeit
rm EID_Q_2021E.pem.crt

wget https://c.sk.ee/EID_Q_2021R.pem.crt
keytool -import -noprompt -v -trustcacerts -file EID_Q_2021R.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias EID-Q-2021R -storepass changeit
rm EID_Q_2021R.pem.crt

wget https://c.sk.ee/EID_Q_2024E.pem.crt
keytool -import -noprompt -v -trustcacerts -file EID_Q_2024E.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias EID-Q-2024E -storepass changeit
rm EID_Q_2024E.pem.crt

wget https://c.sk.ee/EID_Q_2024R.pem.crt
keytool -import -noprompt -v -trustcacerts -file EID_Q_2024R.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias EID-Q-2024R -storepass changeit
rm EID_Q_2024R.pem.crt

wget https://c.sk.ee/SK_ID_Solutions_ROOT_G1E.pem.crt
keytool -import -noprompt -v -trustcacerts -file SK_ID_Solutions_ROOT_G1E.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias SK_ROOT_G1E -storepass changeit
rm SK_ID_Solutions_ROOT_G1E.pem.crt

wget https://c.sk.ee/SK_ID_Solutions_ROOT_G1R.pem.crt
keytool -import -noprompt -v -trustcacerts -file SK_ID_Solutions_ROOT_G1R.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias SK_ROOT_G1R -storepass changeit
rm SK_ID_Solutions_ROOT_G1R.pem.crt

wget https://www.sk.ee/upload/files/EID-SK_2016.pem.crt
keytool -import -noprompt -v -trustcacerts -file EID-SK_2016.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias EID-SK-2016 -storepass changeit
rm EID-SK_2016.pem.crt

wget https://www.skidsolutions.eu/upload/files/ESTEID-SK_2015.pem.crt
keytool -import -noprompt -v -trustcacerts -file ESTEID-SK_2015.pem.crt \
-keystore test_sid_trusted_issuers.jks -alias ESTEID-SK-2015 -storepass changeit
rm ESTEID-SK_2015.pem.crt

wget https://www.sk.ee/upload/files/NQ-SK_2016.pem.crt
keytool -import -noprompt -v -trustcacerts -file NQ-SK_2016.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias NQ-SK-2016 -storepass changeit
rm NQ-SK_2016.pem.crt

wget https://www.sk.ee/upload/files/EE_Certification_Centre_Root_CA.pem.crt
keytool -import -noprompt -v -trustcacerts -file EE_Certification_Centre_Root_CA.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias Centre_Root_CA -storepass changeit
rm EE_Certification_Centre_Root_CA.pem.crt

wget https://www.skidsolutions.eu/upload/files/TEST_EID-NQ_2021E.pem.crt
keytool -import -noprompt -v -trustcacerts \
 -file TEST_EID-NQ_2021E.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias TEST-EID-NQ-2021E -storepass changeit
rm TEST_EID-NQ_2021E.pem.crt

wget https://www.skidsolutions.eu/upload/files/TEST_EID-NQ_2021R.pem.crt
keytool -import -noprompt -v -trustcacerts \
 -file TEST_EID-NQ_2021R.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias TEST-EID-NQ-2021R -storepass changeit
rm TEST_EID-NQ_2021R.pem.crt

wget https://www.skidsolutions.eu/upload/files/TEST_EID-Q_2021E.pem.crt
keytool -import -noprompt -v -trustcacerts -file TEST_EID-Q_2021E.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias TEST-EID-Q-2021E -storepass changeit
rm TEST_EID-Q_2021E.pem.crt

wget https://www.skidsolutions.eu/upload/files/TEST_EID-Q_2021R.pem.crt
keytool -import -noprompt -v -trustcacerts -file TEST_EID-Q_2021R.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias TEST-EID-Q-2021R -storepass changeit
rm TEST_EID-Q_2021R.pem.crt

wget https://c.sk.ee/TEST_of_SK_ID_Solutions_EID-Q_2024E.pem.crt
keytool -import -noprompt -v -trustcacerts -file TEST_of_SK_ID_Solutions_EID-Q_2024E.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias TEST-EID-Q-2024E -storepass changeit
rm TEST_of_SK_ID_Solutions_EID-Q_2024E.pem.crt

wget https://c.sk.ee/TEST_of_SK_ID_Solutions_EID-Q_2024R.pem.crt
keytool -import -noprompt -v -trustcacerts -file TEST_of_SK_ID_Solutions_EID-Q_2024R.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias TEST-EID-Q-2024R -storepass changeit
rm TEST_of_SK_ID_Solutions_EID-Q_2024R.pem.crt

wget https://www.skidsolutions.eu/upload/files/TEST_SK_ROOT_G1_2021E.pem.crt
keytool -import -noprompt -v -trustcacerts -file TEST_SK_ROOT_G1_2021E.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias TEST_SK_ROOT_G1_2021E -storepass changeit
rm TEST_SK_ROOT_G1_2021E.pem.crt

wget https://www.skidsolutions.eu/upload/files/TEST_SK_ROOT_G1_2021R.pem.crt
keytool -import -noprompt -v -trustcacerts -file TEST_SK_ROOT_G1_2021R.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias TEST_SK_ROOT_G1_2021R -storepass changeit
rm TEST_SK_ROOT_G1_2021R.pem.crt

wget https://www.skidsolutions.eu/upload/files/TEST%20of%20EID-SK%202016_reissued.pem
keytool -import -noprompt -v -trustcacerts -file 'TEST of EID-SK 2016_reissued.pem' \
 -keystore test_sid_trusted_issuers.jks -alias TEST-EID-SK-2016 -storepass changeit
rm 'TEST of EID-SK 2016_reissued.pem'

wget https://www.sk.ee/upload/files/TEST_of_ESTEID-SK_2015.pem.crt
keytool -import -noprompt -v -trustcacerts -file TEST_of_ESTEID-SK_2015.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias TEST-ESTEID-SK-2015 -storepass changeit
rm TEST_of_ESTEID-SK_2015.pem.crt

wget https://www.skidsolutions.eu/upload/files/TEST%20of%20NQ-SK%202016_reissued.pem
keytool -import -noprompt -v -trustcacerts -file 'TEST of NQ-SK 2016_reissued.pem' \
 -keystore test_sid_trusted_issuers.jks -alias TEST-NQ-SK-2016 -storepass changeit
rm 'TEST of NQ-SK 2016_reissued.pem'

# certificates 'TEST of EID-SK 2016', 'TEST of NQ-SK 2016' and 'TEST of ESTEID-SK 2015' are
# verified by 'TEST of EE Certification Centre Root CA'
wget https://www.sk.ee/upload/files/TEST_of_EE_Certification_Centre_Root_CA.pem.crt
keytool -import -noprompt -v -trustcacerts -file TEST_of_EE_Certification_Centre_Root_CA.pem.crt \
 -keystore test_sid_trusted_issuers.jks -alias TEST-Centre-Root-CA \
 -storepass changeit
rm TEST_of_EE_Certification_Centre_Root_CA.pem.crt

# add mock service certificate sk-ca.localhost.crt for testing
keytool -import -noprompt -v -trustcacerts -file sk-ca.localhost.crt \
 -keystore test_sid_trusted_issuers.jks -alias sk-ca.localhost -storepass changeit

# add generated certificate cyber-ca.localhost.pem.crt for gatling tests
keytool -import -noprompt -v -trustcacerts -file cyber-ca.localhost.pem.crt \
-keystore test_sid_trusted_issuers.jks -alias cyber-ca.localhost -storepass changeit
