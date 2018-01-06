package org.adrianwalker.uploadserver.configuration.factory;

import org.adrianwalker.ftpserver.usermanager.ldap.LdapUserManager;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LdapUserManagerFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(LdapUserManagerFactory.class);

  private final String host;
  private final int port;
  private final String name;
  private final String credentials;
  private final int maxActive;
  private final int maxIdle;
  private final long timeout;
  private final String userBaseDn;

  public LdapUserManagerFactory(
          final String host, final int port,
          final String name, final String credentials,
          final int maxActive, final int maxIdle,
          final long timeout,
          final String userBaseDn) {

    LOGGER.debug("host = {}, port = {}, name = {}, credentials = {}, maxActive = {}, maxIdle = {}, "
            + "timeout = {}, userBaseDn = {}",
            host, port, name, credentials, maxActive, maxIdle, timeout, userBaseDn);

    if (null == host) {
      throw new IllegalArgumentException("host is null");
    }

    if (0 >= port) {
      throw new IllegalArgumentException("invalid port");
    }

    if (null == credentials) {
      throw new IllegalArgumentException("credentials is null");
    }

    if (0 >= maxActive) {
      throw new IllegalArgumentException("invalid maxActive");
    }

    if (0 >= maxIdle) {
      throw new IllegalArgumentException("invalid maxIdle");
    }

    if (0 >= timeout) {
      throw new IllegalArgumentException("invalid timeout");
    }

    if (null == userBaseDn) {
      throw new IllegalArgumentException("userBaseDn is null");
    }

    this.host = host;
    this.port = port;
    this.name = name;
    this.credentials = credentials;
    this.maxActive = maxActive;
    this.maxIdle = maxIdle;
    this.timeout = timeout;
    this.userBaseDn = userBaseDn;
  }

  public LdapUserManager newInstance() {

    LOGGER.debug("creating instance");

    LdapConnectionConfig config = new LdapConnectionConfig();
    config.setLdapHost(host);
    config.setLdapPort(port);
    config.setName(name);
    config.setCredentials(credentials);

    GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
    poolConfig.maxActive = maxActive;
    poolConfig.maxIdle = maxIdle;

    DefaultLdapConnectionFactory ldapConnectionFactory = new DefaultLdapConnectionFactory(config);
    ldapConnectionFactory.setTimeOut(timeout);
    ValidatingPoolableLdapConnectionFactory poolableLdapConnectionFactory
            = new ValidatingPoolableLdapConnectionFactory(ldapConnectionFactory);
    LdapConnectionPool ldapPool = new LdapConnectionPool(poolableLdapConnectionFactory, poolConfig);
    LdapConnectionTemplate ldapConnectionTemplate = new LdapConnectionTemplate(ldapPool);
    LdapUserManager ldapUserManager = new LdapUserManager(ldapConnectionTemplate, userBaseDn);

    return ldapUserManager;
  }
}
