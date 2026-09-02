#!/bin/sh
# Create the ShowKart Kafka topics with 3 partitions each. Idempotent.
set -eu

BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka:9092}"
KAFKA_TOPICS=/opt/kafka/bin/kafka-topics.sh

for topic in booking.events payment.events inventory.events; do
  echo "Creating topic ${topic}..."
  "${KAFKA_TOPICS}" \
    --bootstrap-server "${BOOTSTRAP}" \
    --create --if-not-exists \
    --topic "${topic}" \
    --partitions 3 \
    --replication-factor 1
done

echo "Topics after init:"
"${KAFKA_TOPICS}" --bootstrap-server "${BOOTSTRAP}" --list
