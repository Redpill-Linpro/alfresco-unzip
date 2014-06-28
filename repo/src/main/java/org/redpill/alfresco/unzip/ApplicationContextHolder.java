package org.redpill.alfresco.unzip;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationContextHolder implements ApplicationContextAware {

  private static ApplicationContext _applicationContext;

  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    ApplicationContextHolder._applicationContext = applicationContext;
  }
  
  public static ApplicationContext getApplicationContext() {
    return _applicationContext;
  }

}
