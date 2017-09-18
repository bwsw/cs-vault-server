**Event processing at the level of acceptance by the ‘consumer’.** <br />

If the event with status "Completed" is deletion of virtual machine, account or creation of virtual machine, user, account then the method of controller will be executed. Otherwise, events must be ignored. <br />

An exception thrown during the processing must would rerun the event processing (except in the case, if the absence of entity creation has caused the exception thrown). <br />


**Event processing at the service interaction level.** <br />

Virtual machine creation event processing: <br />
  * a) getting of appropriated account ID through cloudStackService <br />
  * b) checking of the account node existence in ZooKeeper server <br />
  * c) if the account node does not exist it will be created. Also the tokens will be created in vault service.  If the account node exists, there will be checked the token nodes ("read" and "write"). Tokens are got from nodes or will be created in Vault service in case if the nodes does not exist <br />
  * d) recording of  the created Vault token into Zookeeper node (if token recording throwns an exception, the token will be revoked) <br />
  * e) recording of tokens into UserVM tags at CloudStackServer <br />

Account creation event processing: <br />
  * a) getting of an account users <br />
  * b) getting of a tag list of all users <br />
  * с) If the tag list does not include the account token tags or includes, but not all necessary: <br />
    * 1) account node will be checked for presence in ZooKeeper service. <br />
    * 2) if the account node does not exist it will be created. Also the tokens will be created in vault service.  If the account node exists, there will be checked the token nodes ("read" and "write"). Tokens are got from nodes or will be created in Vault service in case if the nodes does not exist. <br />
    * 3) recording of  the created Vault Token into Zookeeper node (if token recording throwns an exception, the token will be revoked) <br />
    * 4) Tokens will be recorded into account user tags at CloudStackServer (in case if users exist). <br />

User creation event processing: <br />
  * a) getting of an user account <br />
  * b) getting of an account users <br />
  * c) getting of a tag list of all users <br />
  * d) If the tag list does not include the account token tags or includes, but not all necessary: <br />
    * 1) account node will be checked for presence in ZooKeeper service. <br />
    * 2) if the account node does not exist it will be created. Also the tokens will be created in vault service.  If the account node exists, there will be checked the token nodes ("read" and "write"). Tokens are got from nodes or will be created in Vault service in case if the nodes does not exist. <br />
    * 3) recording of  the created Vault Token into Zookeeper node (if token recording throwns an exception, the token will be revoked) <br />
  * e) recording of tokens into tags of new user at CloudStackServer <br />

Account or VM deletion event processing: <br />
  * a) if account or VM node is in ZooKeeper then go to point "b" otherwise point "e" <br />
  * b) if token nodes ("read" and "write") are in ZooKeeper then go to point "c" otherwise point "e" <br />
  * c) getting of tokens from ZooKeeper node <br />
  * d) revoking of tokens from Vault, getting of token secret path <br />
  * e) deleting token secret from  the path or default path
