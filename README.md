# SecretDocs

Augmented Reality Android application that authenticates a user, allows a user to upload confidential documents with specified access and displays confidential documents based on the user's authorization and access levels.

## Demo

https://photos.app.goo.gl/vP1o2vTDhqf9SNQCA

## Brief Description

* The User first signs up, by selecting an access level (in a production system, an administrator will need to approve such a request). * Once signed up, the user can enter their Username (email address) and click authenticate. 
* The User will then have to authenticate themseleves using the AR lock, where the User will have to select the password by moving the phone such that the selection ring displayed covers the number to be entered. 
* A maximum of 5 retries and a character limit of 32 characters have been set for this authentication.
* Once the User is authenticated, they can then upload documents or view documents uploaded by other users. 
* When a User uploads a document, they will be able to send a QR code over email (required to view the document later), and can continue to upload more documents or move on to viewing documents. 
* In order to view a document, if the User has the required access level, they can focus the QR code and fit it in the square box displayed on screen. 
* The app retrieves the document to be displayed and renders the document on top of this QR code. If the User doesn't have the access level, a restricted access image is displayed. 

## Code Overview 

The app is made of 6 activities. The function of each activity is described below.

### LoginActivity

* Launcher activity which takes in a valid Username (email address with '@' character) of the user, and allows the user to authenticate themselves by clicking on the Authenticate button (starts the authentication activity). 
* There is also an option for new users to signup, this starts the signup activity.

### SignUpActivity

* Activity which allows the user to enter a valid email address (with a '@' character), a numeric password and retype the numeric password. 
* On completion, the user may click the sign up button which finishes this activity and sends results (credentials accepted) to the calling (Login) activity.

### AuthActivity

* Activity which provides AR authentication. The authentication mechanism is borrowed from https://github.com/dani-amirtharaj/AR-Lock. Here, the keypad is setup mid-air. 
* Once the key entered by the user matches their password, the next activity, MenuActivity is started, and this activity is finished.
* If the user exceeds the 32 character limit or 5 retries, the actvity finishes, with a message explaining it was unable to authenticate the user.

### MenuActivity

* Activity which allows the user to upload documents (image files) or view existing documents that have already been uploaded and have QR codes generated (starts the SecretDocsActivity). 
* Once a document is uploaded by the user with a specific access level, a QRCode is generated and stored in ARCore's augmented image database (used to detect the QR codes in an AR session, for viewing the secret documents in the next activity).
* The QR code is attached in a pre-generated email (using Android's ACTION_SEND intent) that the user may share with themselves or other users.

### SecretDocsActivity

* Activity that starts an ARCore session and displays virtual documents on top of QR codes. 
* It sets up an AR fragment (AugmentedImageFragment) first (which internally starts the AR session and takes care of UI) and loads the augmented image database, and starts detecting QR codes that were stored in the augmented image database.
* Once a QR code is detected and the User has the required access level (User access level >= document access level), the corresponding document to be displayed is loaded (in AugmentedImageNode) and rendered on top of the QR code. 
* This is done using ARCore's anchors, which help anchor a renderable to a particular location on the detected image (center of QR code in this case). If the User doesn't have the access level, a restricted access image is displayed. 

