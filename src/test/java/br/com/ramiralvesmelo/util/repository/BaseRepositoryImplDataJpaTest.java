package br.com.ramiralvesmelo.util.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import br.com.ramiralvesmelo.util.repository._it.entity.SampleEntity;
import br.com.ramiralvesmelo.util.repository_it.impl.SampleEntityRepository;

@DataJpaTest
@Import(SampleEntityRepository.class)
class BaseRepositoryImplDataJpaTest {

  @Autowired
  SampleEntityRepository repo;

  @Test
  void crudCompletoEmMemoria() {
    // count inicial
    assertThat(repo.count()).isZero();

    // save (merge) — note que BaseRepositoryImpl usa merge, então
    // para inserir, a entidade precisa estar "transiente".
    SampleEntity e = new SampleEntity("Alice");
    e = repo.save(e);
    assertThat(e.getId()).isNotNull();

    // findById
    assertThat(repo.findById(e.getId())).isPresent();

    // findAll
    assertThat(repo.findAll()).hasSize(1);

    // existsById
    assertThat(repo.existsById(e.getId())).isTrue();
       
    // deleteById
    repo.deleteById(e.getId());
    assertThat(repo.existsById(e.getId())).isFalse();
    assertThat(repo.count()).isZero();

    // delete
    e = new SampleEntity("Alice");
    e = repo.save(e);       
    repo.delete(e);
    assertThat(repo.existsById(e.getId())).isFalse();
    assertThat(repo.count()).isZero();

  }
}
