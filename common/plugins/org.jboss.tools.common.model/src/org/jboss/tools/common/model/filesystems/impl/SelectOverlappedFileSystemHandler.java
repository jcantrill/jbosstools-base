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

import java.io.*;
import java.util.*;

import org.jboss.tools.common.meta.action.XActionInvoker;
import org.jboss.tools.common.meta.action.impl.*;
import org.jboss.tools.common.model.*;
import org.jboss.tools.common.model.filesystems.FilePathHelper;
import org.jboss.tools.common.model.filesystems.FileSystemsHelper;
import org.jboss.tools.common.model.impl.*;
import org.jboss.tools.common.model.util.*;

public class SelectOverlappedFileSystemHandler extends AbstractHandler {

    public SelectOverlappedFileSystemHandler() {}

    public boolean isEnabled(XModelObject o) {
        return o != null && (XModelObjectConstants.TRUE.equals(o.get("overlapped"))); //$NON-NLS-1$
    }

    public void executeHandler(XModelObject object, java.util.Properties p) throws XModelException {
        XModelObject fs = getOverlappedFileSystem(object);
        if(object == fs) return;
        if(fs == null) {
        	if(object.isActive() && object.getFileType() == XModelObject.FILE
        			&& XActionInvoker.getAction("OpenFile", object) != null) { //$NON-NLS-1$
       			XActionInvoker.invoke("OpenFile", object, null); //$NON-NLS-1$
        	}
        	return;
        } 
        Properties fsp = XModelObjectUtil.toProperties(fs);
        if(XModelObjectConstants.YES.equals(fsp.getProperty("hidden", XModelObjectConstants.NO))) { //$NON-NLS-1$
            fsp.setProperty("hidden", XModelObjectConstants.NO); //$NON-NLS-1$
            fs.setAttributeValue("info", XModelObjectUtil.toString(fsp)); //$NON-NLS-1$
            fs.setModified(true);
            XModelImpl m = (XModelImpl)object.getModel();
            m.fireStructureChanged(fs.getParent());
        }
        FindObjectHelper.findModelObject(fs, FindObjectHelper.EVERY_WHERE);
    }

    static XModelObject getOverlappedFileSystem(XModelObject source) {
        String fp = getAbsoluteFileFolderPath(source);
        if(fp == null) return null;
        XModelObject fs = FileSystemsHelper.getFileSystems(source.getModel());
        if(fs == null) return null;
        XModelObject[] cs = fs.getChildren();
        for (int i = 0; i < cs.length; i++)
          if(fp.equals(getAbsoluteFileSystemPath(cs[i]))) return cs[i];
        return null;
    }

    private static String getAbsoluteFileSystemPath(XModelObject fso) {
        String path = XModelObjectUtil.getExpandedValue(fso, XModelObjectConstants.ATTR_NAME_LOCATION, null);
        try {
        	path = new File(path).getCanonicalPath().replace('\\', '/');
            return FilePathHelper.toPathPath(path);
        } catch (IOException e) {
        	//ignore
            return null;
        }
    }

    private static String getAbsoluteFileFolderPath(XModelObject f) {
        String path = f.getPath();
        String rpath = XModelObjectLoaderUtil.getResourcePath(f);
		if(path == null || rpath == null) return null;
        XModelObject fso = f.getModel().getByPath(path.substring(0, path.length() - rpath.length()));
        String pp = getAbsoluteFileSystemPath(fso) + rpath;
        return FilePathHelper.toPathPath(pp);
    }

}

