package com.oneandone.typedrest.vaadin;

import static com.oneandone.typedrest.BeanUtils.getPropertiesWithAnnotation;
import com.oneandone.typedrest.ListerHidden;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.CustomComponent;
import java.util.*;

/**
 * Common base class for entity lister implementations.
 *
 * @param <TEntity> The type of entities the list shows.
 */
public abstract class AbstractEntityLister<TEntity>
        extends CustomComponent implements EntityLister<TEntity> {

    protected final Class<TEntity> entityType;
    protected final BeanItemContainer<TEntity> container;

    /**
     * Creates a new entity lister.
     *
     * @param entityType The type of entities the list shows.
     */
    protected AbstractEntityLister(Class<TEntity> entityType) {
        this.entityType = entityType;

        this.container = new BeanItemContainer<>(entityType);
        getPropertiesWithAnnotation(entityType, ListerHidden.class)
                .forEach(x -> container.removeContainerProperty(x.getName()));
    }

    @Override
    public void setEntities(Collection<TEntity> entities) {
        container.removeAllItems();
        container.addAll(entities);
    }

    @Override
    public void addEntity(TEntity entity) {
        container.addBean(entity);
    }

    private final Collection<EntityClickListener<TEntity>> clickListeners = new LinkedList<>();

    @Override
    public void addEntityClickListener(EntityClickListener<TEntity> listener) {
        clickListeners.add(listener);
    }

    protected void onClick(TEntity entity) {
        clickListeners.forEach(x -> x.entityClick(entity));
    }
}
