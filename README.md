# SecretDocs

Augmented Reality Android application that authenticates a user, allows user to upload confidential documents and displays confidential documents based on the user's authorization and access levels.

## Code Overview 

The app is made of 6 activities. The function of each activity is described below.

* LoginActivity

Launcher activity which takes in valid Username (email address with '@' character) of the user, and allows the user to authenticate themselves by clicking on the Authenticate button (starts the authentication activity). There is also an option for new users to signup, this starts the signup activity.

* SignUpActivity

Activity which allows the user to enter a valid email address (with a '@' character), a numeric password and retype the numeric password. On completion, the user may click the sign up button which finishes this activity and sends results (credentials accepted) to the calling (Login) activity.

* AuthActivity

Activity which provides AR authentication. The authentication mechanism is borrowed from https://github.com/dani-amirtharaj/AR-Lock. Once the key entered by the user matches their password, the next activity, MenuActivity is started, and this activity is finished.

* MenuActivity

Activity which allows the user to upload documents (image files) or view existing documents that have already been uploaded (starts the SecretDocsActivity) and have QR codes generated. Once a document is uploaded, a QRCode is generated and stored in ARCore's augmented image database (used to detect the QR codes in an AR session, for viewing the secret documents)  and is attached in a pre-generated email (using Android's ACTION_SEND intent) and the user may share this with themselves or other users.

* SecretDocsActivity

Activity that starts ARCore session and displays virtual documents on top of QR codes. It sets up the AR fragment (AugmentedImageFragment) first which loads the augmented image database, and starts detecting QR codes that were stored in the augmented image database. Once a QR code is detected, the corresponding document to be displayed is loaded (in AugmentedImageNode) and if the User has the required access level, it is displayed. If the User doesn't have the access level, a restricted access image is displayed. 

