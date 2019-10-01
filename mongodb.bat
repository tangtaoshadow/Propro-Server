D:
cd \Program Files\MongoDB\Server\4.0\bin
mongod --bind_ip 0.0.0.0 --setParameter failIndexKeyTooLong=false --wiredTigerCacheSizeGB 6