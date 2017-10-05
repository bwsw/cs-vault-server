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
    * 1) the tokens are created in vault <br />
    * 2) zookeeper nodes are created for keeping VM tokens <br />
    * 3) write the vault tokens to zookeeper (in case of an exception, the tokens will be revoked) <br />
  * d) write the tokens in CloudStack as UserVM tags <br />

The processing of account creation event consists of the following steps: <br />
  * a) get the account users <br />
  * b) get a tag list of each user and select only vault tags <br />
  * с) if the list of tokens is empty or does not contain both vault tokens ('RO' token, 'RW' token, 'vaultPrefix', 'vaultUrl'): <br />
    * 1) try to retrieve account tokens from zookeeper <br />
    * 2) if the tokens don't exist: <br />
      * a) the tokens are created in vault <br />
      * b) zookeeper nodes are created for keeping account tokens <br />
      * c) write the vault tokens to zookeeper (in case of an exception, the tokens will be revoked) <br />
    * 3) create 'vaultPrefix' or 'vaultUrl' tags if it does not exist <br />
    * 4) write the tag in CloudStack as user tags for users which do not have all tags for vault data  <br />

The processing of user creation event consists of the following steps: <br />
  * a) get an user account <br />
  * b) get the account users <br />
  * c) get a tag list of each user and select only token tags <br />
  * d) if the list of tokens is empty or does not contain both account tokens ('RO' token and 'RW' token): <br />
    * 1) try to retrieve account tokens from zookeeper <br />
    * 2) if the tokens don't exist: <br />
      * a) the tokens are created in vault <br />
      * b) zookeeper nodes are created for keeping account tokens <br />
      * c) write the vault tokens to zookeeper (in case of an exception, the tokens will be revoked) <br />
    * 3) create 'vaultPrefix' or 'vaultUrl' tags if it does not exist <br />
    * 4) write the tag in CloudStack as user tags for users which do not have all tags for vault data  <br />

The processing of VM/account deletion event consists of the following steps: <br />
  * a) if VM/account node exists in zookeeper: <br />
    * 1) try to retrieve tokens from zookeeper <br />
    * 2) if the tokens exist: <br />
      * 1) get a policy of each token from vault and delete it <br />
      * 2) revoke the tokens in vault <br />
  * b) delete VM/account node from zookeeper (with all sub-nodes) <br />
  * c) delete the secrets using path configured by vmsVaultBasicPath/accountsVaultBasicPath setting (see application.conf) <br />
