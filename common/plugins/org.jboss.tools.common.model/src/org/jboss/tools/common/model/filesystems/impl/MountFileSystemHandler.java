/*******************************************************************************
 * Copyright (c) 2007 Exadel, Inc. and Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
 ******************************************************************************/ 
package org.jboss.tools.common.model.filesystems.impl;

import java.text.MessageFormat;
import java.util.*;
import java.io.*;

import org.jboss.tools.common.meta.action.*;
import org.jboss.tools.common.meta.action.impl.handlers.DefaultCreateHandler;
import org.jboss.tools.common.model.*;
import org.jboss.tools.common.model.plugin.ModelMessages;
import org.jboss.tools.common.model.util.*;

public class MountFileSystemHandler extends DefaultCreateHandler {

    public MountFileSystemHandler() {}

    public void executeHandler(XModelObject object, Properties p) throws XModelException {
        if(!isEnabled(object) || data == null || data.length == 0) return;
        String entity = data[0].getModelEntity().getName();
        p = extractProperties(data[0]);
        if(!checkOverlap(object, entity, p)) return;
        setRelativeToProject(object, p);
        mount(object, p, entity);
    }

    public XModelObject mount(XModelObject fs, Properties p, String entity) throws XModelException {
        validateName(fs, p);
        XModelObject c = XModelObjectLoaderUtil.createValidObject(fs.getModel(), entity, p);
        addCreatedObject(fs, c, false, p);
        fs.getModel().getUndoManager().addUndoable(new MountFileSystemUndo(c));
        updateClassPath(c);
		MoveFileSystemHandler.sortFileSystems(fs.getModel());
        return c;
    }

    private boolean checkOverlap(XModelObject object, String entity, Properties p) {
        String location = p.getProperty(XModelObjectConstants.ATTR_NAME_LOCATION);
        if(location == null) return true;
        boolean b = location.indexOf('%') >= 0;
        location = canonize(location, object.getModel());
        if(location != null && !b) p.setProperty(XModelObjectConstants.ATTR_NAME_LOCATION, location);
        if(!XModelObjectConstants.ENT_FILE_SYSTEM_FOLDER.equals(entity)) return true;
        location += XModelObjectConstants.SEPARATOR;
        XModelObject[] cs = object.getChildren(entity);
        for (int i = 0; i < cs.length; i++) {
            String loc = canonize(cs[i].get(XModelObjectConstants.ATTR_NAME_LOCATION), cs[i].getModel()) + XModelObjectConstants.SEPARATOR;
            if(!loc.startsWith(location) && !location.startsWith(loc)) continue;
            String mes = MessageFormat.format(
					"File system {0} will share files with file system {1}",
					p.get(XModelObjectConstants.ATTR_NAME), cs[i].getAttributeValue(XModelObjectConstants.ATTR_NAME));
            ServiceDialog d = object.getModel().getService();
            int q = d.showDialog(ModelMessages.WARNING, mes, new String[]{ModelMessages.OK, ModelMessages.Cancel}, null, ServiceDialog.WARNING);
            return (q == 0);
        }
        return true;
    }

    private String canonize(String location, XModel model) {
        try {
            location = XModelObjectUtil.expand(location, model, null);
            return location == null ? null : (new File(location).getCanonicalPath()).replace('\\', '/');
        } catch (IOException e) {
            return location;
        }
    }

    private void validateName(XModelObject object, Properties p) {
        String name = p.getProperty(XModelObjectConstants.ATTR_NAME);
        if(name != null && name.length() > 0) return;
        String location = p.getProperty(XModelObjectConstants.ATTR_NAME_LOCATION);
        name = location.substring(location.lastIndexOf('/') + 1);
        if(name.length() == 0) name = "filesystem"; //$NON-NLS-1$
        name = XModelObjectUtil.createNewChildName(name, object);
        p.setProperty(XModelObjectConstants.ATTR_NAME, name);
    }

    private void setRelativeToProject(XModelObject object, Properties p) {
        boolean isRelative = XModelObjectConstants.TRUE.equals(p.getProperty("set location relative to project")); //$NON-NLS-1$
        if(!isRelative) return;
        String location = canonize(p.getProperty(XModelObjectConstants.ATTR_NAME_LOCATION), object.getModel());
        String project = canonize(XModelConstants.WORKSPACE_REF, object.getModel());
        if(location.equals(project)) {
            p.setProperty(XModelObjectConstants.ATTR_NAME_LOCATION, XModelConstants.WORKSPACE_REF);
            return;
        }
        boolean common = false;
        while(location.length() > 0 && project.length() > 0) {
            int i1 = location.indexOf('/');
            if(i1 < 0) i1 = location.length();
            int i2 = project.indexOf('/');
            if(i2 < 0) i2 = project.length();
            if(i1 != i2) break;
            String p1 = location.substring(0, i1);
            String p2 = project.substring(0, i2);
            if(!p1.equals(p2)) break;
            location = location.substring(i1);
            project = project.substring(i2);
            if(location.startsWith(XModelObjectConstants.SEPARATOR)) location = location.substring(1);
            if(project.startsWith(XModelObjectConstants.SEPARATOR)) project = project.substring(1);
            common = true;
        }
        if(!common) return;
        String s = XModelConstants.WORKSPACE_REF;
        if(project.length() > 0) {
            int q = new StringTokenizer(project, XModelObjectConstants.SEPARATOR).countTokens();
            for (int i = 0; i < q; i++) s += "/.."; //$NON-NLS-1$
        }
        if(location.length() > 0) s += XModelObjectConstants.SEPARATOR + location;
        p.setProperty(XModelObjectConstants.ATTR_NAME_LOCATION, s);
    }

	static SpecialWizard w = SpecialWizardFactory.createSpecialWizard("org.jboss.tools.common.model.project.ClassPathUpdateWizard"); //$NON-NLS-1$

    public static void updateClassPath(XModelObject fs) {
		if(fs.getModelEntity().getName().indexOf("ar") >= 0 && w != null) { //$NON-NLS-1$
			Properties p = new Properties();
			p.put("model", fs.getModel()); //$NON-NLS-1$
			w.setObject(p);
			w.execute();
		}
    }

}

