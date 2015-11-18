package com.oneandone.typedrest.vaadin.sample.components;

import com.oneandone.typedrest.sample.endpoints.*;
import com.oneandone.typedrest.sample.models.*;
import com.oneandone.typedrest.vaadin.*;
import com.vaadin.ui.Window;

public class ResourceRevisionCollectionComponent
        extends AbstractCollectionComponent<ResourceRevision, ResourceRevisionCollection, ResourceRevisionElement> {

    public ResourceRevisionCollectionComponent(ResourceRevisionCollection endpoint) {
        super(endpoint);
        setCaption("Revisions");
    }

    @Override
    protected Window buildCreateElementWindow() {
        return new CreateResourceRevisionElementComponent(endpoint).asWindow();
    }

    @Override
    protected Window buildUpdateElementWindow(ResourceRevisionElement elementEndpoint) {
        return new UpdateResourceRevisionElementComponent(elementEndpoint).asWindow();
    }
}
