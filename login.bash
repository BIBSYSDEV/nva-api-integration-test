#!/bin/bash

# Sjekk om en SSO-profil er angitt som argument
if [ -z "$1" ]; then
    echo "Bruk: $0 <aws_sso_profile>"
    echo "Vennligst oppgi navnet på AWS SSO-profilen du vil logge inn med."
    exit 1
fi

AWS_PROFILE=$1

# Logg inn med AWS SSO
echo "Logger inn med AWS SSO for profilen: $AWS_PROFILE..."
aws sso login --profile $AWS_PROFILE

# Sjekk om innloggingen var vellykket
if [ $? -ne 0 ]; then
    echo "Innlogging med AWS SSO mislyktes for profilen '$AWS_PROFILE'. Sjekk konfigurasjonen din."
    exit 1
fi

# Hent midlertidige legitimasjonsbevis fra SSO-profilen
echo "Henter midlertidige AWS-legitimasjonsbevis..."
CREDS=$(aws configure export-credentials --profile $AWS_PROFILE)
export AWS_ACCESS_KEY_ID=$(echo $CREDS | jq -r '.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDS | jq -r '.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDS | jq -r '.SessionToken')

# Sjekk om legitimasjonsbevisene ble funnet
if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ] || [ -z "$AWS_SESSION_TOKEN" ]; then
    echo "Kunne ikke hente legitimasjonsbevis for profilen '$AWS_PROFILE'."
    exit 1
fi

echo "Legitimasjonsbevis er lagret som miljøvariabler:"
