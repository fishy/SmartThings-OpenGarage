# SmartThings and OpenGarage

This repository provides a [SmartThings](https://www.smartthings.com/)
Device Handler for [OpenGarage](https://opengarage.io),
and two SmartApps to help you automate your garage door.

## Device Handler

[open-garage.groovy](open-garage.groovy)

First you need to setup your [OpenGarage](https://opengarage.io) normally,
including the [Blynk](https://blynk.cc) integration steps.

Then, login to your
[SmartThings developer account](https://graph.api.smartthings.com/),
create a new Device Handler, and import the code.

After that, you can create a new Device using the Device Handler you just
created.
On your SmartThings app, choose "Add a Thing" and the device you just created
from developer IDE will appear. Confirm it.

There are 3 required preferences you must set before you can really use the
device. You can set them up on either the app or IDE:

### Blynk Auth Token

This is the auth token Blynk sent you during setup.

### Blynk URL prefix

As at the time of writing blynk doesn't provide a proper HTTPS API so you have
to use the HTTP version of `http://blynk-cloud.com`.

If you would like to use HTTPS for better security,
you'll need a special HTTPS proxy.
Refer to my [blynk-proxy](https://github.com/fishy/blynk-proxy) project for more
details.
You could setup your own proxy using blynk-proxy,
or if after understanding the
[risks](https://github.com/fishy/blynk-proxy/blob/master/README.md#should-i-use-your-heroku-app)
you still doesn't mind, you could use `https://blynk-proxy.herokuapp.com`.

### State Refresh Rate

We couldn't push the garage door state to SmartThings right now so the update of
the state initiate rely on refresh requests from SmartThings to Blynk server.
This preference controls how frequent we send the refresh requests.

If you don't have a Contact Sensor setup with your OpenGarage,
you would need a more frequent refresh rate to keep the state up-to-date.

## Garage Door with Contact Sensor SmartApp

[garage-with-contact-sensor.groovy](garage-with-contact-sensor.groovy)

This is a SmartApp to help you better get more up-to-date state of your
OpenGarage
(or any other unofficially supported garage device that doesn't needs pull).

First you'll need a separated Contact Sensor installed on your garage door.

Then, login to your
[SmartThings developer account](https://graph.api.smartthings.com/),
create a new SmartApp, and import the code.

After that, on your SmartThings app you can add the SmartApp,
select your OpenGarage as the garage door and the contact sensor.

The idea behind this SmartApp is that whenever the contact sensor's state
changes, it will ask the garage door to refresh its state,
so your garage door's state will update in seconds instead of minutes.

## Presence and Garage Door SmartApp

[presence-and-garage-door.groovy](presence-and-garage-door.groovy)

This is a SmartApp to automate your garage door with a presence sensor
(probably in your car).
The basic idea is that when the presence sensor's state changed to not present,
that means your car is leaving so it closes the garage door;
When the presence sensor's state changed to present,
that means your car is arriving so it opens the garage door.

One tricky thing is that presence sensor may report false state changes.
When the presence sensor loses connection to the hub,
the state will change to not present and the SmartApp will try to close your
garage door.
This is not a big deal,
but when it restore the connection to the hub,
the state will change to present and the SmartApp will try to open your garage
door, and that's a thing to avoid.

To resolve this false report problem,
there's an option called "Real close threshold" in this SmartApp.
When set, for example to 300 seconds (5 minutes),
when your presence sensor's state changed to not present,
the SmartApp will check the door's current state,
and if it's already closed, check what's the time it actually closed.
If the actual close time is more than the threshold,
the SmartApp assumes that this is a false report,
and won't really open the garage door when its state next change to present.

Refer to the above section about how to install this SmartApp.
