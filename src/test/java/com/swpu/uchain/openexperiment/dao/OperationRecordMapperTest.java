package com.swpu.uchain.openexperiment.dao;

import com.swpu.uchain.openexperiment.DTO.OperationRecordDTO;
import com.swpu.uchain.openexperiment.domain.OperationRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@SpringBootTest
@RunWith(SpringRunner.class)
public class OperationRecordMapperTest {

    @Autowired
    private OperationRecordMapper recordMapper;

    @Test
    public void deleteByPrimaryKey() {
    }

    @Test
    public void insert() {

        List<OperationRecordDTO> list = new ArrayList<>();
        OperationRecordDTO operationRecordDTO = new OperationRecordDTO();
        operationRecordDTO.setOperationContent("1");
        operationRecordDTO.setOperationReason("满足要求");
        operationRecordDTO.setOperationType("1");
        operationRecordDTO.setRelatedId(1L);
        list.add(operationRecordDTO);
        System.err.println(recordMapper.multiInsert(list));

    }

    @Test
    public void selectByPrimaryKey() {
    }

    @Test
    public void selectAll() {
    }

    @Test
    public void updateByPrimaryKey() {
    }
}