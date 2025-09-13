package br.com.ramiralvesmelo.util.repository_it.impl;

import org.springframework.stereotype.Repository;

import br.com.ramiralvesmelo.util.repository.BaseRepositoryImpl;
import br.com.ramiralvesmelo.util.repository._it.entity.SampleEntity;

@Repository
public class SampleEntityRepository extends BaseRepositoryImpl<SampleEntity, Long> { 
}
