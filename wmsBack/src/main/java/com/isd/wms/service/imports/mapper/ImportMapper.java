package com.isd.wms.service.imports.mapper;

public interface ImportMapper<T, E> {
    E toEntity(T dto);
    Class<T> supports();
}
