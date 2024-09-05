# Scheduling a Zoom Video SDK session using the Zoom Calendar API
A simple scheduling app with an in-memory database that uses the Zoom Calendar API to schedule Zoom Video SDK sessions and perform basic validations.

## Prerequisites
1. A Zoom account with Zoom Mail and Calendar activated so you have a calendar id (your @zmail.com address)
2. A Zoom Marketplace app on this account (Server-to-server) for your account id, client id and secret.
3. A Zoom Video SDK account
4. A Zoom Marketplace app on this account for your video sdk key and secret.

## Running locally
Prerequisite: JDK 22+ installed. May work on lower versions, but this is what the repo is tested with.
1. Clone this repo
2. Copy the `.env.example` file to a new file called `.env`
```shell
cp .env.example .env
```
3. Replace the placeholders in the `.env` file with your actual values
4. Run the app
```shell
source .env && ./mvnw spring-boot:run
```
5. Application will be available at http://localhost:8080

## Usage
1. Navigate to http://localhost:8080 and enter some valid details
2. Open your Zoom (@zmail) account in the Zoom Client. This should show a freshly created item in your calendar.
3. Open the calendar item and click the session join link. This should go back to the app and start a Zoom Video SDK session.

## Validations that apply
1. Can't join a session past its planned end date/time.
2. Can't join a session earlier than 15 minutes before its planned start.
3. Can't start a session if the session name is unknown, or the passcode doesn't match the stored one

## Tips
* There's a handy database console at http://localhost:8080/h2-console
* Use the default password from `application.properties` to log in