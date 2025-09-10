package br.com.ramiralvesmelo.util.repository.impl;

import org.springframework.stereotype.Repository;

import br.com.ramiralvesmelo.util.repository.BaseRepositoryImpl;
import br.com.ramiralvesmelo.util.repository.entity.SampleEntity;

@Repository
public class SampleEntityRepository extends BaseRepositoryImpl<SampleEntity, Long> { 
}
