AWS Mobile App Sync Starter App for Android

# Introduction

This is a Starter application for using the Sample app in the AWS AppSync console when building your GraphQL API. The Sample app creates a GraphQL schema and provisions Amazon DynamoDB resources, then connects them appropriately with Resolvers. The application demonstrates GraphQL Mutations, Queries, and Offline support using AWS AppSync. You can use this for learning purposes or adapt either the application or the GraphQL Schema to meet your needs.

<img src="media/event_details.png" width="50%"></img>

## Features

- GraphQL Mutations
  - Create new events
  - Create comments on existing events
- GraphQL Queries
  - Get all events
  - Get an event by Id
- Authorization
  - The app uses API Key as the authorization mechanism

## AWS Setup

1. Navigate to the AWS AppSync console using the URL: http://console.aws.amazon.com/appsync/home

2. Click on `Create API` and select the `Sample Schema` option. Enter a API name of your choice. Click `Create`.

## Android Setup

1. Clone this repository:

	```
	git clone https://github.com/aws-samples/aws-mobile-appsync-events-starter-android.git
	```

2. Open Android Studio, choose `Import project` navigate to the repository folder that was cloned and select open.

3. Inside Android Studio, choose the menu `Tools > Android > Sync Project with Gradle Files` to ensure gradle is up to date and wait until this completes.

4. From the homepage of your GraphQL API (you can click the name you entered in the left hand navigation) wait until the progress bar at the top has completed deploying your resources. 

	On this same page, the `API Details` box at the top of the page will contain the `API URL` and `API Key` that you will paste into the `Constants.java` file.

## Application walkthrough

### Generated code

Java code is generated from a schema file (`./app/src/main/graphql/com/amazonaws/demo/appsync/schema.json`) and a .graphql file (`/app/src/main/graphql/com/amazonaws/demo/appsync/events.graphql`) based on your API. The generated source is in the `./app/build/generated/source/appsync` folder of this project after a build is completed.

If you update your schema in the future, you will find updated versions of these in the AWS AppSync console under the homepage for your GraphQL API when you click the `Android` tab.

### ListEventsActivity.java (Query)

- The `ListEventsActivity.java` file lists all the events accessible to the user. It returns data from the offline cache first if available and later fetches it from remote to update the local cache.

### ViewEventActivity.java (Mutation, Query, Subscription)

- The `ViewEventActivity.java` file lists information about an event and allows new comments to be added. New comments to the event are added while the user is viewing the event via subscriptions.

### AddEventActivity.java (Mutation)

- The `AddEventActivity.java` file creates a new event using the details entered on screen.

## License

This library is licensed under the Amazon Software License.
