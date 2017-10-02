**Event processing at the consumption level.** <br />

A controller handles the following events if they have "Completed" status. 
1) creation of virtual machine, account and user
2) deletion of virtual machine and account 

Other events are ignored. <br />

In case of an exception, the processing of event will be restarted, except the creation event which has no entity in CloudStack (see the first paragraph above). <br />


**Event processing at the service interaction level.** <br />

The processing of VM creation event consists of the following steps: <br />
  * a) get the appropriate account ID via CloudStackService <br />
  * b) try to retrieve tokens from zookeeper. Token can have 'RO' or 'RW' policy <br />
  * c) if the tokens don't exist: <br />
    * 1) the tokens will be created in vault <br />
    * 2) zookeeper nodes will be created for keeping VM tokens <br />
    * 3) write the vault tokens to zookeeper (In case of an exception, the tokens will be revoked) <br />
  * d) write the tokens in CloudStack as UserVM tags <br />

The processing of account creation event consists of the following steps: <br />
  * a) getting of an account users <br />
  * b) getting of a tag list of all users <br />
  * с) If the tag list does not include the account token tags or includes, but not all necessary: <br />
    * 1) account node will be checked for presence in ZooKeeper service. <br />
    * 2) if the account node does not exist it will be created. Also the tokens will be created in vault service.  If the account node exists, there will be checked the token nodes ("read" and "write"). Tokens are got from nodes or will be created in Vault service in case if the nodes does not exist. <br />
    * 3) recording of  the created Vault Token into Zookeeper node (if token recording throwns an exception, the token will be revoked) <br />
    * 4) Tokens will be recorded into account user tags at CloudStackServer (in case if users exist). <br />

The processing of user creation event consists of the following steps: <br />
  * a) getting of an user account <br />
  * b) getting of an account users <br />
  * c) getting of a tag list of all users <br />
  * d) If the tag list does not include the account token tags or includes, but not all necessary: <br />
    * 1) account node will be checked for presence in ZooKeeper service. <br />
    * 2) if the account node does not exist it will be created. Also the tokens will be created in vault service.  If the account node exists, there will be checked the token nodes ("read" and "write"). Tokens are got from nodes or will be created in Vault service in case if the nodes does not exist. <br />
    * 3) recording of  the created Vault Token into Zookeeper node (if token recording throwns an exception, the token will be revoked) <br />
  * e) recording of tokens into tags of new user at CloudStackServer <br />

The processing of VM or account deletion event consists of the following steps: <br />
  * a) if account or VM node is in ZooKeeper then go to point "b" otherwise point "e" <br />
  * b) if token nodes ("read" and "write") are in ZooKeeper then go to point "c" otherwise point "e" <br />
  * c) getting of tokens from ZooKeeper node <br />
  * d) revoking of tokens from Vault, getting of token secret path <br />
  * e) deleting token secret from  the path or default path
