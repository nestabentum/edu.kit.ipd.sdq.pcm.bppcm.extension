package de.uhd.ifi.se.pcm.bppcm.core;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.osgi.framework.Bundle;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.resourcetype.ResourceRepository;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;

import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.OrganizationEnvironmentModel;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.OrganizationenvironmentmodelPackage;
import edu.kit.ipd.sdq.eventsim.api.PCMModel;


/**
 * Responsible for loading the organization environment model from bundle
 * 
 * @author Robert Heinrich
 * 
 */
public class BPPCMModel extends PCMModel{

	

	public BPPCMModel(Allocation allocationModel, Repository repositoryModel, ResourceEnvironment resourceModel,
			System systemModel, UsageModel usageModel, ResourceRepository resourceRepository) {
		super(allocationModel, repositoryModel, resourceModel, systemModel, usageModel, resourceRepository);
		
	}

	public static OrganizationEnvironmentModel loadFromBundle(final Bundle bundle,
            final IPath businessProcessModelLocation) {

        final URI bpmUri = relativePathToBundleURI(bundle, businessProcessModelLocation);
        
        // the organization environment model is not required in all cases
        // if only IT is simulated, the organization environment model is not required
        if(bpmUri != null){

        	final ResourceSet resourceSet = new ResourceSetImpl();
        	resourceSet.getResource(bpmUri, true);
        	EcoreUtil.resolveAll(resourceSet);

        	OrganizationEnvironmentModel organizationModel = null;

        	for (final Resource r : resourceSet.getResources()) {
        		final EObject o = r.getContents().get(0);
        		if (OrganizationenvironmentmodelPackage.eINSTANCE.getOrganizationEnvironmentModel().isInstance(o)) {
        			organizationModel = (OrganizationEnvironmentModel) o;
        		}
        	}

        	return organizationModel;
    }
  
    
    return null;

    }
}
