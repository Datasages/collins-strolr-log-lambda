package com.collins.railwaynet.strolrloglambda.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import com.collins.railwaynet.strolrloglambda.entity.LogFile;


public class HibernateUtil {
    private static SessionFactory sessionFactory;
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
        	try {
                Configuration configuration = new Configuration();
                ServiceRegistry serviceRegistry
                    = new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties()).build();
                configuration.addAnnotatedClass(LogFile.class);
                return configuration
                        .buildSessionFactory(serviceRegistry);
            }catch(Exception e) {
                e.printStackTrace();
                throw new RuntimeException("There is issue in hibernate util");
            }
        }
        System.out.println("Session Factory open: " + sessionFactory.isOpen());
        return sessionFactory;
    }
}